ALTER TABLE education_career
    ADD COLUMN IF NOT EXISTS is_employed BOOLEAN DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS working_state VARCHAR(100),
    ADD COLUMN IF NOT EXISTS working_pincode VARCHAR(12);

UPDATE education_career
SET is_employed = CASE
    WHEN COALESCE(NULLIF(BTRIM(occupation), ''), NULLIF(BTRIM(annual_income), ''), NULLIF(BTRIM(working_city), '')) IS NOT NULL
        THEN TRUE
    ELSE COALESCE(is_employed, FALSE)
END
WHERE is_employed IS DISTINCT FROM CASE
    WHEN COALESCE(NULLIF(BTRIM(occupation), ''), NULLIF(BTRIM(annual_income), ''), NULLIF(BTRIM(working_city), '')) IS NOT NULL
        THEN TRUE
    ELSE COALESCE(is_employed, FALSE)
END;

CREATE INDEX IF NOT EXISTS idx_education_career_working_state
ON education_career(working_state);
