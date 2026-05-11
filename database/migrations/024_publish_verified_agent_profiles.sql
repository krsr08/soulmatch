-- Agent-created member profiles must become member-visible after verification.
-- This backfills profiles that were reviewed or had required documents verified
-- before the admin publish path updated is_published/review_status consistently.

WITH verified_document_profiles AS (
    SELECT
        p.profile_id,
        BOOL_OR(pd.document_type = 'aadhaar' AND pd.status = 'verified') AS has_aadhaar,
        BOOL_OR(pd.document_type IN ('pan', 'voter_id') AND pd.status = 'verified') AS has_pan_or_voter,
        BOOL_OR(pd.document_type = 'education_certificate' AND pd.status = 'verified') AS has_education_certificate,
        BOOL_OR(pd.document_type = 'divorce_decree' AND pd.status = 'verified') AS has_divorce_decree,
        COALESCE(p.review_status, 'draft') AS review_status,
        p.submitted_at,
        NULLIF(BTRIM(COALESCE(ec.education_level, '')), '') IS NOT NULL
            OR NULLIF(BTRIM(COALESCE(ec.occupation, '')), '') IS NOT NULL
            OR NULLIF(BTRIM(COALESCE(ec.annual_income, '')), '') IS NOT NULL AS has_education_claim,
        COALESCE(p.marital_status, '') ILIKE '%divorce%' AS requires_divorce_decree
    FROM profiles p
    LEFT JOIN profile_documents pd ON pd.profile_id = p.profile_id
    LEFT JOIN education_career ec ON ec.profile_id = p.profile_id
    WHERE p.created_by_advisor_id IS NOT NULL
    GROUP BY p.profile_id, p.review_status, p.submitted_at, p.marital_status, ec.education_level, ec.occupation, ec.annual_income
)
UPDATE profiles p
SET is_published = TRUE,
    verification_status = 'verified',
    review_status = 'verified',
    admin_status = 'active',
    verified_at = COALESCE(p.verified_at, NOW()),
    reviewed_at = COALESCE(p.reviewed_at, NOW()),
    rejection_reason = NULL,
    updated_at = NOW()
FROM verified_document_profiles vdp
WHERE p.profile_id = vdp.profile_id
  AND p.created_by_advisor_id IS NOT NULL
  AND COALESCE(p.admin_status, 'active') = 'active'
  AND (
      vdp.submitted_at IS NOT NULL
      OR vdp.review_status IN ('submitted', 'under_review', 'verified')
  )
  AND (
      COALESCE(p.verification_status, 'pending') = 'verified'
      OR (
          vdp.has_aadhaar
          AND vdp.has_pan_or_voter
          AND (NOT vdp.has_education_claim OR vdp.has_education_certificate)
          AND (NOT vdp.requires_divorce_decree OR vdp.has_divorce_decree)
      )
  );
