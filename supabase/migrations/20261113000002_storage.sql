-- Public-read bucket for choice images. Objects live under "<uid>/<file>"; users may only write/delete their own folder.
insert into storage.buckets (id, name, public, file_size_limit, allowed_mime_types)
values ('choice-images', 'choice-images', true, 2097152, array['image/jpeg','image/png','image/webp'])
on conflict (id) do nothing;

create policy choice_images_insert on storage.objects for insert to authenticated
  with check (bucket_id = 'choice-images' and (storage.foldername(name))[1] = (select auth.uid())::text);

create policy choice_images_delete on storage.objects for delete to authenticated
  using (bucket_id = 'choice-images' and (storage.foldername(name))[1] = (select auth.uid())::text);
