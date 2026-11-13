-- end_voting now accepts the host's raffle pick, but only if it is among the top-voted choices.
drop function if exists public.end_voting(text);

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

  if p_winner is not null then
    select id into v_winner from public.choices
    where room_id = p_room and id = p_winner and vote_count = v_max;
    if v_winner is null then raise exception 'winner must be a top-voted choice'; end if;
  else
    select id into v_winner from public.choices
    where room_id = p_room and vote_count = v_max
    order by random() limit 1;
  end if;

  update public.rooms set state = 'RESULT', winner_choice_id = v_winner where id = p_room;
  return v_winner;
end;
$$;

revoke all on function public.end_voting(text, text) from public, anon;
grant execute on function public.end_voting(text, text) to authenticated;
