-- Storage needs a SELECT policy for an object's owner before it allows the owner to delete it.
-- (Public reads by URL are unaffected; this only lets a user see/list their own folder.)
create policy choice_images_select_own on storage.objects for select to authenticated
  using (bucket_id = 'choice-images' and (storage.foldername(name))[1] = (select auth.uid())::text);
