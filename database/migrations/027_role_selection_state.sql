ALTER TABLE users
    ADD COLUMN IF NOT EXISTS role_selected_at TIMESTAMP;

UPDATE users u
SET role_selected_at = COALESCE(u.role_selected_at, u.created_at, NOW())
WHERE u.role_selected_at IS NULL
  AND (
    u.user_type IN ('agent', 'admin')
    OR EXISTS (SELECT 1 FROM profiles p WHERE p.user_id = u.user_id)
    OR EXISTS (SELECT 1 FROM advisors a WHERE a.user_id = u.user_id)
  );

CREATE INDEX IF NOT EXISTS idx_users_role_selection_state
    ON users(user_type, role_selected_at);
