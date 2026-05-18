ALTER TABLE profile_photos
    ADD COLUMN IF NOT EXISTS review_status VARCHAR(24) DEFAULT 'pending',
    ADD COLUMN IF NOT EXISTS review_comment TEXT,
    ADD COLUMN IF NOT EXISTS reviewed_at TIMESTAMP,
    ADD COLUMN IF NOT EXISTS reviewed_by VARCHAR(255);

UPDATE profile_photos
SET review_status = CASE WHEN COALESCE(is_approved, false) THEN 'approved' ELSE COALESCE(review_status, 'pending') END
WHERE review_status IS NULL OR review_status = '';

ALTER TABLE profile_photos DROP CONSTRAINT IF EXISTS profile_photos_review_status_check;
ALTER TABLE profile_photos
    ADD CONSTRAINT profile_photos_review_status_check
    CHECK (review_status IN ('pending', 'approved', 'rejected'));

CREATE INDEX IF NOT EXISTS idx_profile_photos_review_status
    ON profile_photos (review_status, uploaded_at DESC);
