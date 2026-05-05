ALTER TABLE profiles
    ADD COLUMN IF NOT EXISTS hide_last_seen BOOLEAN DEFAULT FALSE;

UPDATE profiles
SET hide_last_seen = COALESCE(hide_last_seen, FALSE)
WHERE hide_last_seen IS NULL;
