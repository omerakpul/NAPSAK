-- NAPSAK: initial schema, RLS, RPCs, realtime
-- Clients never write tables directly; all mutations go through SECURITY DEFINER RPCs.

-- ───────────── Tables ─────────────
create table public.rooms (
  id               text primary key check (id ~ '^[0-9]{6}$'),
  host_id          uuid not null,
  state            text not null default 'WAITING' check (state in ('WAITING','VOTING','RESULT')),
  winner_choice_id text,
  created_at       timestamptz not null default now()
);

create table public.participants (
  room_id   text not null references public.rooms(id) on delete cascade,
  user_id   uuid not null,
  name      text not null check (char_length(name) between 1 and 40),
  is_ready  boolean not null default false,
  has_voted boolean not null default false,
  joined_at timestamptz not null default now(),
  primary key (room_id, user_id)
);

create table public.choices (
  room_id    text not null references public.rooms(id) on delete cascade,
  id         text not null,
  name       text not null check (char_length(name) between 1 and 100),
  details    text not null default '' check (char_length(details) <= 500),
  image_url  text,
  latitude   double precision,
  longitude  double precision,
  category   text not null default '',
  vote_count integer not null default 0,
  position   integer not null default 0,
  primary key (room_id, id)
);

-- Secret ballot: clients have no SELECT policy on this table.
create table public.votes (
  room_id   text not null,
  choice_id text not null,
  user_id   uuid not null,
  primary key (room_id, choice_id, user_id),
  foreign key (room_id, choice_id) references public.choices(room_id, id) on delete cascade
);

create index on public.rooms (host_id);
create index on public.rooms (created_at);

-- ───────────── RLS ─────────────
alter table public.rooms        enable row level security;
alter table public.participants enable row level security;
alter table public.choices      enable row level security;
alter table public.votes        enable row level security;

create or replace function public.is_member(p_room text)
returns boolean
language sql stable security definer set search_path = ''
as $$
  select exists (
    select 1 from public.participants
    where room_id = p_room and user_id = (select auth.uid())
  );
$$;

create policy rooms_select        on public.rooms        for select to authenticated
  using (host_id = (select auth.uid()) or public.is_member(id));
create policy participants_select on public.participants for select to authenticated
  using (public.is_member(room_id));
create policy choices_select      on public.choices      for select to authenticated
  using (public.is_member(room_id));
-- no insert/update/delete policies anywhere: writes only via RPCs below.

-- ───────────── RPCs ─────────────
create or replace function public.create_room(p_name text)
returns text
language plpgsql security definer set search_path = ''
as $$
declare
  v_uid uuid := auth.uid();
  v_code text;
  v_try int := 0;
begin
  if v_uid is null then raise exception 'not authenticated'; end if;
  loop
    v_try := v_try + 1;
    v_code := (floor(random() * 900000) + 100000)::int::text;
    begin
      insert into public.rooms (id, host_id) values (v_code, v_uid);
      exit;
    exception when unique_violation then
      if v_try >= 10 then raise exception 'could not allocate room code'; end if;
    end;
  end loop;
  insert into public.participants (room_id, user_id, name, is_ready)
  values (v_code, v_uid, left(trim(p_name), 40), true);
  return v_code;
end;
$$;

create or replace function public.join_room(p_room text, p_name text)
returns text
language plpgsql security definer set search_path = ''
as $$
declare
  v_uid uuid := auth.uid();
  v_state text;
  v_count int;
begin
  if v_uid is null then raise exception 'not authenticated'; end if;
  select state into v_state from public.rooms where id = p_room;
  if v_state is null then raise exception 'room not found'; end if;

  if not exists (select 1 from public.participants where room_id = p_room and user_id = v_uid) then
    if v_state <> 'WAITING' then raise exception 'voting already started'; end if;
    select count(*) into v_count from public.participants where room_id = p_room;
    if v_count >= 50 then raise exception 'room is full'; end if;
  end if;

  insert into public.participants (room_id, user_id, name)
  values (p_room, v_uid, left(trim(p_name), 40))
  on conflict (room_id, user_id) do update set name = excluded.name;
  return p_room;
end;
$$;

create or replace function public.set_ready(p_room text, p_ready boolean)
returns void
language plpgsql security definer set search_path = ''
as $$
begin
  update public.participants set is_ready = p_ready
  where room_id = p_room and user_id = auth.uid();
end;
$$;

-- p_choices: [{id?, name, details?, image_url?, latitude?, longitude?, category?}, ...]
create or replace function public.start_voting(p_room text, p_choices jsonb)
returns void
language plpgsql security definer set search_path = ''
as $$
declare
  v_n int := coalesce(jsonb_array_length(p_choices), 0);
begin
  if not exists (select 1 from public.rooms where id = p_room and host_id = auth.uid() and state = 'WAITING') then
    raise exception 'only the host can start voting from the lobby';
  end if;
  if v_n < 1 or v_n > 50 then raise exception 'choices must be between 1 and 50'; end if;

  insert into public.choices (room_id, id, name, details, image_url, latitude, longitude, category, position)
  select p_room,
         coalesce(nullif(c->>'id', ''), gen_random_uuid()::text),
         left(c->>'name', 100),
         left(coalesce(c->>'details', ''), 500),
         nullif(c->>'image_url', ''),
         (c->>'latitude')::double precision,
         (c->>'longitude')::double precision,
         coalesce(c->>'category', ''),
         (ord - 1)::int
  from jsonb_array_elements(p_choices) with ordinality as t(c, ord);

  update public.rooms set state = 'VOTING' where id = p_room;
end;
$$;

create or replace function public.submit_votes(p_room text, p_choice_ids text[])
returns void
language plpgsql security definer set search_path = ''
as $$
declare
  v_uid uuid := auth.uid();
  v_ids text[] := coalesce((select array_agg(distinct x) from unnest(p_choice_ids) x), '{}');
  v_valid int;
begin
  if not exists (select 1 from public.rooms where id = p_room and state = 'VOTING') then
    raise exception 'room is not in voting state';
  end if;
  if not exists (select 1 from public.participants where room_id = p_room and user_id = v_uid) then
    raise exception 'not a participant';
  end if;
  if exists (select 1 from public.participants where room_id = p_room and user_id = v_uid and has_voted) then
    raise exception 'already voted';
  end if;

  select count(*) into v_valid from public.choices where room_id = p_room and id = any(v_ids);
  if v_valid <> coalesce(array_length(v_ids, 1), 0) then raise exception 'invalid choice id'; end if;

  insert into public.votes (room_id, choice_id, user_id)
  select p_room, x, v_uid from unnest(v_ids) x;

  update public.choices set vote_count = vote_count + 1
  where room_id = p_room and id = any(v_ids);

  update public.participants set has_voted = true where room_id = p_room and user_id = v_uid;
end;
$$;

-- Winner = most votes; ties broken randomly on the server.
create or replace function public.end_voting(p_room text)
returns text
language plpgsql security definer set search_path = ''
as $$
declare v_winner text;
begin
  if not exists (select 1 from public.rooms where id = p_room and host_id = auth.uid() and state = 'VOTING') then
    raise exception 'only the host can end voting';
  end if;
  select id into v_winner from public.choices
  where room_id = p_room
  order by vote_count desc, random()
  limit 1;
  update public.rooms set state = 'RESULT', winner_choice_id = v_winner where id = p_room;
  return v_winner;
end;
$$;

create or replace function public.leave_room(p_room text)
returns void
language plpgsql security definer set search_path = ''
as $$
begin
  if exists (select 1 from public.rooms where id = p_room and host_id = auth.uid()) then
    delete from public.rooms where id = p_room;   -- host leaving closes the room
  else
    delete from public.participants where room_id = p_room and user_id = auth.uid();
  end if;
end;
$$;

create or replace function public.delete_room(p_room text)
returns void
language plpgsql security definer set search_path = ''
as $$
begin
  delete from public.rooms where id = p_room and host_id = auth.uid();
end;
$$;

-- housekeeping: drop rooms older than 24h (schedule with pg_cron)
create or replace function public.cleanup_old_rooms()
returns int
language plpgsql security definer set search_path = ''
as $$
declare n int;
begin
  delete from public.rooms where created_at < now() - interval '24 hours';
  get diagnostics n = row_count;
  return n;
end;
$$;

-- ───────────── Privileges ─────────────
revoke all on function public.create_room(text)            from public, anon;
revoke all on function public.join_room(text, text)        from public, anon;
revoke all on function public.set_ready(text, boolean)     from public, anon;
revoke all on function public.start_voting(text, jsonb)    from public, anon;
revoke all on function public.submit_votes(text, text[])   from public, anon;
revoke all on function public.end_voting(text)             from public, anon;
revoke all on function public.leave_room(text)             from public, anon;
revoke all on function public.delete_room(text)            from public, anon;
revoke all on function public.is_member(text)              from public, anon;
revoke all on function public.cleanup_old_rooms()          from public, anon, authenticated;

grant execute on function public.create_room(text)          to authenticated;
grant execute on function public.join_room(text, text)      to authenticated;
grant execute on function public.set_ready(text, boolean)   to authenticated;
grant execute on function public.start_voting(text, jsonb)  to authenticated;
grant execute on function public.submit_votes(text, text[]) to authenticated;
grant execute on function public.end_voting(text)           to authenticated;
grant execute on function public.leave_room(text)           to authenticated;
grant execute on function public.delete_room(text)          to authenticated;
grant execute on function public.is_member(text)            to authenticated;

-- ───────────── Realtime ─────────────
alter table public.participants replica identity full;
alter table public.choices      replica identity full;
alter publication supabase_realtime add table public.rooms, public.participants, public.choices;
