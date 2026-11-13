-- Vote modes (LIKE / SINGLE / RANK) and participant suggestions.

-- ───────────── Vote modes ─────────────
alter table public.rooms add column if not exists vote_mode text not null default 'LIKE'
  check (vote_mode in ('LIKE', 'SINGLE', 'RANK'));
-- points per vote: 1 for LIKE/SINGLE, Borda points (n - rank + 1) for RANK; choices.vote_count holds the sum.
alter table public.votes add column if not exists points integer not null default 1;

drop function if exists public.start_voting(text, jsonb);

create or replace function public.start_voting(p_room text, p_choices jsonb, p_mode text default 'LIKE')
returns void
language plpgsql security definer set search_path = ''
as $$
declare
  v_n int := coalesce(jsonb_array_length(p_choices), 0);
begin
  if p_mode not in ('LIKE', 'SINGLE', 'RANK') then raise exception 'invalid vote mode'; end if;
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

  delete from public.suggestions where room_id = p_room;
  update public.rooms set state = 'VOTING', vote_mode = p_mode where id = p_room;
end;
$$;

-- LIKE: any set of choices (deduplicated). SINGLE: exactly one. RANK: an ordered list, best first.
create or replace function public.submit_votes(p_room text, p_choice_ids text[])
returns void
language plpgsql security definer set search_path = ''
as $$
declare
  v_uid uuid := auth.uid();
  v_mode text;
  v_ids text[];
  v_total int;
  v_valid int;
begin
  select vote_mode into v_mode from public.rooms where id = p_room and state = 'VOTING';
  if v_mode is null then raise exception 'room is not in voting state'; end if;
  if not exists (select 1 from public.participants where room_id = p_room and user_id = v_uid) then
    raise exception 'not a participant';
  end if;
  if exists (select 1 from public.participants where room_id = p_room and user_id = v_uid and has_voted) then
    raise exception 'already voted';
  end if;

  if v_mode = 'LIKE' then
    v_ids := coalesce((select array_agg(distinct x) from unnest(p_choice_ids) x), '{}');
  else
    v_ids := coalesce(p_choice_ids, '{}');
    if cardinality(v_ids) <> (select count(distinct x) from unnest(v_ids) x) then
      raise exception 'duplicate choice in vote';
    end if;
    if v_mode = 'SINGLE' and cardinality(v_ids) <> 1 then raise exception 'pick exactly one choice'; end if;
  end if;

  select count(*) into v_valid from public.choices where room_id = p_room and id = any(v_ids);
  if v_valid <> coalesce(cardinality(v_ids), 0) then raise exception 'invalid choice id'; end if;
  select count(*) into v_total from public.choices where room_id = p_room;

  with src as (
    select x as cid,
           case when v_mode = 'RANK' then v_total - ord + 1 else 1 end::int as pts
    from unnest(v_ids) with ordinality as t(x, ord)
  ), ins as (
    insert into public.votes (room_id, choice_id, user_id, points)
    select p_room, cid, v_uid, pts from src
    returning choice_id, points
  )
  update public.choices c set vote_count = c.vote_count + ins.points
  from ins where c.room_id = p_room and c.id = ins.choice_id;

  update public.participants set has_voted = true where room_id = p_room and user_id = v_uid;
end;
$$;

revoke all on function public.start_voting(text, jsonb, text) from public, anon;
revoke all on function public.submit_votes(text, text[]) from public, anon;
grant execute on function public.start_voting(text, jsonb, text) to authenticated;
grant execute on function public.submit_votes(text, text[]) to authenticated;

-- ───────────── Suggestions ─────────────
create table if not exists public.suggestions (
  room_id    text not null references public.rooms(id) on delete cascade,
  id         uuid not null default gen_random_uuid(),
  user_id    uuid not null,
  name       text not null check (char_length(name) between 1 and 100),
  details    text not null default '' check (char_length(details) <= 500),
  created_at timestamptz not null default now(),
  primary key (room_id, id)
);
alter table public.suggestions enable row level security;

-- the host sees every suggestion, everyone else only their own
create policy suggestions_select on public.suggestions for select to authenticated
  using (
    user_id = (select auth.uid())
    or exists (select 1 from public.rooms r where r.id = room_id and r.host_id = (select auth.uid()))
  );

create or replace function public.suggest_choice(p_room text, p_name text, p_details text default '')
returns void
language plpgsql security definer set search_path = ''
as $$
declare v_uid uuid := auth.uid();
begin
  if not exists (select 1 from public.rooms where id = p_room and state = 'WAITING') then
    raise exception 'suggestions are only open in the lobby';
  end if;
  if not exists (select 1 from public.participants where room_id = p_room and user_id = v_uid) then
    raise exception 'not a participant';
  end if;
  if char_length(trim(coalesce(p_name, ''))) = 0 then raise exception 'name required'; end if;
  if (select count(*) from public.suggestions where room_id = p_room and user_id = v_uid) >= 5 then
    raise exception 'suggestion limit reached';
  end if;
  insert into public.suggestions (room_id, user_id, name, details)
  values (p_room, v_uid, left(trim(p_name), 100), left(trim(coalesce(p_details, '')), 500));
end;
$$;

-- the host can dismiss any suggestion, a participant only their own
create or replace function public.dismiss_suggestion(p_room text, p_id uuid)
returns void
language plpgsql security definer set search_path = ''
as $$
begin
  delete from public.suggestions s
  where s.room_id = p_room and s.id = p_id
    and (s.user_id = auth.uid()
         or exists (select 1 from public.rooms r where r.id = p_room and r.host_id = auth.uid()));
end;
$$;

revoke all on function public.suggest_choice(text, text, text) from public, anon;
revoke all on function public.dismiss_suggestion(text, uuid) from public, anon;
grant execute on function public.suggest_choice(text, text, text) to authenticated;
grant execute on function public.dismiss_suggestion(text, uuid) to authenticated;

create trigger suggestions_touch_room after insert on public.suggestions
  for each row execute function public.touch_room();

alter table public.suggestions replica identity full;
alter publication supabase_realtime add table public.suggestions;
