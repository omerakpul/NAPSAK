-- end_voting: a stale/non-top p_winner no longer aborts; the server falls back to a random top-voted choice.
-- Returns the winner id actually stored so clients can show the authoritative result.
create or replace function public.end_voting(p_room text, p_winner text default null)
returns text
language plpgsql security definer set search_path = ''
as $$
declare
  v_max int;
  v_winner text;
begin
  if not exists (select 1 from public.rooms where id = p_room and host_id = auth.uid() and state = 'VOTING') then
    raise exception 'only the host can end voting';
  end if;

  select max(vote_count) into v_max from public.choices where room_id = p_room;

  select id into v_winner from public.choices
  where room_id = p_room and id = p_winner and vote_count = v_max;

  if v_winner is null then
    select id into v_winner from public.choices
    where room_id = p_room and vote_count = v_max
    order by random() limit 1;
  end if;

  update public.rooms set state = 'RESULT', winner_choice_id = v_winner where id = p_room;
  return v_winner;
end;
$$;

-- Hourly housekeeping of abandoned rooms (cascades to participants/choices/votes).
create extension if not exists pg_cron with schema pg_catalog;
select cron.schedule('napsak-cleanup-old-rooms', '17 * * * *', $$select public.cleanup_old_rooms()$$);
