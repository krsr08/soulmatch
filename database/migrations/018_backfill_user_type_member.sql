UPDATE users
SET user_type = 'member'
WHERE user_type IS NULL
   OR trim(user_type) = '';

UPDATE users u
SET user_type = 'member'
WHERE COALESCE(trim(u.user_type), '') NOT IN ('member', 'agent', 'admin')
  AND EXISTS (
      SELECT 1
      FROM profiles p
      WHERE p.user_id = u.user_id
  );

UPDATE users u
SET user_type = 'member'
WHERE EXISTS (
      SELECT 1
      FROM profiles p
      WHERE p.user_id = u.user_id
  )
  AND NOT EXISTS (
      SELECT 1
      FROM advisors a
      WHERE a.user_id = u.user_id
  )
  AND u.user_type <> 'member';
