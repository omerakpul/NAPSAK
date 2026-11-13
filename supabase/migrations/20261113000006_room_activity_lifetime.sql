-- Rooms now live as long as they are used: cleanup is based on last activity (3 days) instead of creation time (24h).
alter table public.rooms add column if not exists last_activity_at timestamptz not null default now();
create index if not exists rooms_last_activity_idx on public.rooms (last_activity_at);

-- Touch the room when something happens in it, at most once a minute (avoids a Realtime event per vote).
create or replace function public.touch_room()
returns trigger
language plpgsql security definer set search_path = ''
as $$
declare v_room text := coalesce(new.room_id, old.room_id);
begin
  update public.rooms set last_activity_at = now()
  where id = v_room and last_activity_at < now() - interval '1 minute';
  return null;
end;
$$;

create trigger participants_touch_room after insert or update or delete on public.participants
  for each row execute function public.touch_room();
create trigger choices_touch_room after insert on public.choices
  for each row execute function public.touch_room();
create trigger votes_touch_room after insert on public.votes
  for each row execute function public.touch_room();

-- state / winner changes always count as activity
create or replace function public.rooms_touch_self()
returns trigger
language plpgsql set search_path = ''
as $$
begin
  new.last_activity_at := now();
  return new;
end;
$$;
create trigger rooms_touch_self before update of state, winner_choice_id on public.rooms
  for each row execute function public.rooms_touch_self();

create or replace function public.cleanup_old_rooms()
returns int
language plpgsql security definer set search_path = ''
as $$
declare n int;
begin
  delete from public.rooms where last_activity_at < now() - interval '3 days';
  get diagnostics n = row_count;
  return n;
end;
$$;
revoke all on function public.touch_room(), public.rooms_touch_self(), public.cleanup_old_rooms() from public, anon, authenticated;
