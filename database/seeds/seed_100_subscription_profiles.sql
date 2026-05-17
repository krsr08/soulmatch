-- QA seed for subscription-limit testing. Run manually against non-production databases only.
-- Creates/updates 100 diverse member profiles:
--   - 50 male, 50 female
--   - 10 requested Indian states
--   - 9 requested languages
--   - Bronze/Silver/Gold/Platinum plan coverage
--   - Photo privacy and contact masking scenarios
--   - Usage counters for monthly profile/contact/shortlist/interest limits
--
-- The seed is intentionally idempotent. Re-running it updates the same QA users,
-- profiles, photos, profile details, subscription rows, and QA reporting view.

WITH seed AS (
  SELECT
    i,
    ('71000000-0000-4000-8000-' || LPAD(i::text, 12, '0'))::uuid AS user_id,
    ('72000000-0000-4000-8000-' || LPAD(i::text, 12, '0'))::uuid AS profile_id,
    ('73000000-0000-4000-8000-' || LPAD(i::text, 12, '0'))::uuid AS photo_id,
    CASE WHEN i <= 50 THEN 'male' ELSE 'female' END AS gender,
    CASE
      WHEN i % 4 = 1 THEN 'bronze'
      WHEN i % 4 = 2 THEN 'silver'
      WHEN i % 4 = 3 THEN 'gold'
      ELSE 'platinum'
    END AS subscription_level,
    (ARRAY[
      'Maharashtra','Tamil Nadu','Karnataka','Delhi','Gujarat',
      'Kerala','Punjab','Telangana','West Bengal','Rajasthan'
    ])[((i - 1) % 10) + 1] AS state_name,
    (ARRAY[
      'Mumbai','Chennai','Bengaluru','New Delhi','Ahmedabad',
      'Kochi','Chandigarh','Hyderabad','Kolkata','Jaipur'
    ])[((i - 1) % 10) + 1] AS city_name,
    (ARRAY[
      'Hindi','Tamil','Telugu','Kannada','Marathi',
      'Bengali','Punjabi','Gujarati','Malayalam'
    ])[((i - 1) % 9) + 1] AS language_name,
    (ARRAY[
      'Aarav','Vivaan','Aditya','Arjun','Reyansh',
      'Kabir','Rohan','Karthik','Rahul','Siddharth'
    ])[((i - 1) % 10) + 1] AS male_first_name,
    (ARRAY[
      'Aadhya','Ananya','Diya','Isha','Kavya',
      'Meera','Nisha','Priya','Saanvi','Veda'
    ])[((i - 1) % 10) + 1] AS female_first_name,
    (ARRAY[
      'Sharma','Reddy','Iyer','Patel','Nair',
      'Singh','Gupta','Das','Kulkarni','Menon'
    ])[((i - 1) % 10) + 1] AS last_name,
    (ARRAY[
      'Software Engineer','Doctor','Chartered Accountant','Product Manager','Teacher',
      'Architect','Civil Engineer','Bank Manager','Entrepreneur','Government Officer'
    ])[((i - 1) % 10) + 1] AS occupation_name,
    (ARRAY[
      'Graduate','Post Graduate','MBA','B.Tech','M.Tech',
      'MBBS','CA','B.Com','M.Sc','PhD'
    ])[((i - 1) % 10) + 1] AS education_name,
    (ARRAY[
      'Below 3L','3-5 LPA','5-10 LPA','10-20 LPA','20L+'
    ])[((i - 1) % 5) + 1] AS income_range,
    (ARRAY['Brahmin','Reddy','Vaishya','Iyer','Patel','Nair','Punjabi','Gujarati','Bengali','Rajput'])[((i - 1) % 10) + 1] AS caste_name
  FROM generate_series(1, 100) AS i
),
prepared AS (
  SELECT
    *,
    CASE WHEN gender = 'male' THEN male_first_name ELSE female_first_name END AS first_name,
    '+9198' || LPAD(i::text, 8, '0') AS phone,
    'qa.subscription.member.' || LPAD(i::text, 3, '0') || '@soulmatch.local' AS email,
    CASE
      WHEN gender = 'male' THEN (ARRAY[
        'https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?auto=format&fit=crop&w=1080&h=1080&q=82',
        'https://images.unsplash.com/photo-1500648767791-00dcc994a43e?auto=format&fit=crop&w=1080&h=1080&q=82',
        'https://images.unsplash.com/photo-1506794778202-cad84cf45f1d?auto=format&fit=crop&w=1080&h=1080&q=82',
        'https://images.unsplash.com/photo-1527980965255-d3b416303d12?auto=format&fit=crop&w=1080&h=1080&q=82',
        'https://images.unsplash.com/photo-1519085360753-af0119f7cbe7?auto=format&fit=crop&w=1080&h=1080&q=82',
        'https://images.unsplash.com/photo-1547425260-76bcadfb4f2c?auto=format&fit=crop&w=1080&h=1080&q=82',
        'https://images.unsplash.com/photo-1560250097-0b93528c311a?auto=format&fit=crop&w=1080&h=1080&q=82',
        'https://images.unsplash.com/photo-1530268729831-4b0b9e170218?auto=format&fit=crop&w=1080&h=1080&q=82',
        'https://images.unsplash.com/photo-1568602471122-7832951cc4c5?auto=format&fit=crop&w=1080&h=1080&q=82',
        'https://images.unsplash.com/photo-1504257432389-52343af06ae3?auto=format&fit=crop&w=1080&h=1080&q=82'
      ])[((i - 1) % 10) + 1]
      ELSE (ARRAY[
        'https://images.unsplash.com/photo-1494790108377-be9c29b29330?auto=format&fit=crop&w=1080&h=1080&q=82',
        'https://images.unsplash.com/photo-1534528741775-53994a69daeb?auto=format&fit=crop&w=1080&h=1080&q=82',
        'https://images.unsplash.com/photo-1524504388940-b1c1722653e1?auto=format&fit=crop&w=1080&h=1080&q=82',
        'https://images.unsplash.com/photo-1508214751196-bcfd4ca60f91?auto=format&fit=crop&w=1080&h=1080&q=82',
        'https://images.unsplash.com/photo-1544005313-94ddf0286df2?auto=format&fit=crop&w=1080&h=1080&q=82',
        'https://images.unsplash.com/photo-1517841905240-472988babdf9?auto=format&fit=crop&w=1080&h=1080&q=82',
        'https://images.unsplash.com/photo-1544725176-7c40e5a71c5e?auto=format&fit=crop&w=1080&h=1080&q=82',
        'https://images.unsplash.com/photo-1529626455594-4ff0802cfb7e?auto=format&fit=crop&w=1080&h=1080&q=82',
        'https://images.unsplash.com/photo-1554151228-14d9def656e4?auto=format&fit=crop&w=1080&h=1080&q=82',
        'https://images.unsplash.com/photo-1512316609839-ce289d3eba0a?auto=format&fit=crop&w=1080&h=1080&q=82'
      ])[((i - 51) % 10) + 1]
    END AS image_url,
    CASE WHEN i % 7 = 0 THEN 'request_only' ELSE 'all' END AS photo_privacy_value,
    CASE WHEN i % 6 = 0 THEN 'masked' ELSE 'visible' END AS contact_privacy_value,
    CASE WHEN i % 5 = 0 THEN 'mediator' ELSE 'self' END AS profile_created_by_value,
    CASE WHEN i % 4 = 0 THEN 'verified' ELSE 'pending' END AS verification_status_value,
    (CURRENT_DATE - ((23 + (i % 15))::text || ' years')::interval - ((i % 330)::text || ' days')::interval)::date AS dob_value,
    CASE WHEN gender = 'female' THEN 152 + (i % 24) ELSE 164 + (i % 26) END AS height_cm_value,
    CASE WHEN gender = 'female' THEN 48 + (i % 18) ELSE 62 + (i % 24) END AS weight_kg_value,
    CASE WHEN i % 4 = 0 THEN 'vegetarian' WHEN i % 4 = 1 THEN 'non_vegetarian' WHEN i % 4 = 2 THEN 'eggetarian' ELSE 'jain' END AS diet_value
  FROM seed
),
upsert_users AS (
  INSERT INTO users (
    user_id, phone, email, is_verified, is_active, user_type, role_selected_at,
    acquisition_source, last_login, created_at, updated_at
  )
  SELECT
    user_id,
    phone,
    email,
    TRUE,
    TRUE,
    'member',
    NOW(),
    'qa_seed_100_subscription_profiles',
    NOW() - ((i % 14)::text || ' days')::interval,
    NOW() - ((i % 60)::text || ' days')::interval,
    NOW()
  FROM prepared
  ON CONFLICT (phone)
  DO UPDATE SET
    email = EXCLUDED.email,
    is_verified = TRUE,
    is_active = TRUE,
    user_type = 'member',
    role_selected_at = COALESCE(users.role_selected_at, NOW()),
    acquisition_source = EXCLUDED.acquisition_source,
    last_login = EXCLUDED.last_login,
    updated_at = NOW()
  RETURNING user_id, phone, email
),
user_rows AS (
  SELECT p.*, u.user_id AS persisted_user_id
  FROM prepared p
  JOIN upsert_users u ON u.phone = p.phone
),
upsert_profiles AS (
  INSERT INTO profiles (
    profile_id, user_id, first_name, last_name, dob, gender, religion, caste,
    mother_tongue, marital_status, completion_score, is_published,
    is_partner_pref_set, profile_status, profile_created_by, verification_status,
    admin_status, primary_photo_url, photo_privacy, contact_privacy,
    profile_visibility, hide_last_seen, created_at, updated_at
  )
  SELECT
    profile_id,
    persisted_user_id,
    first_name,
    last_name,
    dob_value,
    gender,
    'Hindu',
    caste_name,
    language_name,
    'never_married',
    100,
    TRUE,
    TRUE,
    'active',
    profile_created_by_value,
    verification_status_value,
    'active',
    image_url,
    photo_privacy_value,
    contact_privacy_value,
    'all',
    (i % 9 = 0),
    NOW() - ((i % 60)::text || ' days')::interval,
    NOW()
  FROM user_rows
  ON CONFLICT (profile_id)
  DO UPDATE SET
    user_id = EXCLUDED.user_id,
    first_name = EXCLUDED.first_name,
    last_name = EXCLUDED.last_name,
    dob = EXCLUDED.dob,
    gender = EXCLUDED.gender,
    religion = EXCLUDED.religion,
    caste = EXCLUDED.caste,
    mother_tongue = EXCLUDED.mother_tongue,
    marital_status = EXCLUDED.marital_status,
    completion_score = EXCLUDED.completion_score,
    is_published = EXCLUDED.is_published,
    is_partner_pref_set = EXCLUDED.is_partner_pref_set,
    profile_status = EXCLUDED.profile_status,
    profile_created_by = EXCLUDED.profile_created_by,
    verification_status = EXCLUDED.verification_status,
    admin_status = EXCLUDED.admin_status,
    primary_photo_url = EXCLUDED.primary_photo_url,
    photo_privacy = EXCLUDED.photo_privacy,
    contact_privacy = EXCLUDED.contact_privacy,
    profile_visibility = EXCLUDED.profile_visibility,
    hide_last_seen = EXCLUDED.hide_last_seen,
    updated_at = NOW()
  RETURNING profile_id, user_id
),
profile_rows AS (
  SELECT ur.*, up.profile_id AS persisted_profile_id
  FROM user_rows ur
  JOIN upsert_profiles up ON up.profile_id = ur.profile_id
),
upsert_physical AS (
  INSERT INTO physical_details (
    profile_id, height_cm, weight_kg, complexion, body_type, blood_group
  )
  SELECT
    persisted_profile_id,
    height_cm_value,
    weight_kg_value,
    CASE WHEN i % 3 = 0 THEN 'Fair' WHEN i % 3 = 1 THEN 'Wheatish' ELSE 'Dusky' END,
    CASE WHEN i % 3 = 0 THEN 'Slim' WHEN i % 3 = 1 THEN 'Average' ELSE 'Athletic' END,
    (ARRAY['O+','A+','B+','AB+'])[((i - 1) % 4) + 1]
  FROM profile_rows
  ON CONFLICT (profile_id)
  DO UPDATE SET
    height_cm = EXCLUDED.height_cm,
    weight_kg = EXCLUDED.weight_kg,
    complexion = EXCLUDED.complexion,
    body_type = EXCLUDED.body_type,
    blood_group = EXCLUDED.blood_group
  RETURNING profile_id
),
upsert_education AS (
  INSERT INTO education_career (
    profile_id, education_level, occupation, annual_income, working_city,
    is_employed, working_state, working_pincode
  )
  SELECT
    persisted_profile_id,
    education_name,
    occupation_name,
    income_range,
    city_name,
    TRUE,
    state_name,
    '5' || LPAD(i::text, 5, '0')
  FROM profile_rows
  ON CONFLICT (profile_id)
  DO UPDATE SET
    education_level = EXCLUDED.education_level,
    occupation = EXCLUDED.occupation,
    annual_income = EXCLUDED.annual_income,
    working_city = EXCLUDED.working_city,
    is_employed = EXCLUDED.is_employed,
    working_state = EXCLUDED.working_state,
    working_pincode = EXCLUDED.working_pincode
  RETURNING profile_id
),
upsert_family AS (
  INSERT INTO family_details (
    profile_id, father_occupation, mother_occupation, num_brothers,
    num_sisters, family_type, family_city, family_state, family_locality,
    family_pincode
  )
  SELECT
    persisted_profile_id,
    CASE WHEN i % 2 = 0 THEN 'Business' ELSE 'Service' END,
    CASE WHEN i % 3 = 0 THEN 'Teacher' ELSE 'Homemaker' END,
    i % 3,
    (i + 1) % 3,
    CASE WHEN i % 2 = 0 THEN 'nuclear' ELSE 'joint' END,
    city_name,
    state_name,
    city_name || ' Central',
    '6' || LPAD(i::text, 5, '0')
  FROM profile_rows
  ON CONFLICT (profile_id)
  DO UPDATE SET
    father_occupation = EXCLUDED.father_occupation,
    mother_occupation = EXCLUDED.mother_occupation,
    num_brothers = EXCLUDED.num_brothers,
    num_sisters = EXCLUDED.num_sisters,
    family_type = EXCLUDED.family_type,
    family_city = EXCLUDED.family_city,
    family_state = EXCLUDED.family_state,
    family_locality = EXCLUDED.family_locality,
    family_pincode = EXCLUDED.family_pincode
  RETURNING profile_id
),
upsert_lifestyle AS (
  INSERT INTO lifestyle_details (
    profile_id, diet, smoking, drinking, about_me
  )
  SELECT
    persisted_profile_id,
    diet_value,
    'never',
    CASE WHEN i % 8 = 0 THEN 'socially' ELSE 'never' END,
    first_name || ' is a ' || education_name || ' profile based in ' || city_name ||
      ', working as a ' || occupation_name ||
      '. This QA profile is generated for subscription, masking, and match-card testing.'
  FROM profile_rows
  ON CONFLICT (profile_id)
  DO UPDATE SET
    diet = EXCLUDED.diet,
    smoking = EXCLUDED.smoking,
    drinking = EXCLUDED.drinking,
    about_me = EXCLUDED.about_me
  RETURNING profile_id
),
upsert_horoscope AS (
  INSERT INTO horoscope_details (
    profile_id, rashi, nakshatra, is_manglik, birth_city, gotra
  )
  SELECT
    persisted_profile_id,
    (ARRAY['Mesha','Vrishabha','Mithuna','Karka','Simha','Kanya'])[((i - 1) % 6) + 1],
    (ARRAY['Ashwini','Rohini','Mrigashira','Pushya','Magha','Hasta'])[((i - 1) % 6) + 1],
    i % 7 = 0,
    city_name,
    caste_name
  FROM profile_rows
  ON CONFLICT (profile_id)
  DO UPDATE SET
    rashi = EXCLUDED.rashi,
    nakshatra = EXCLUDED.nakshatra,
    is_manglik = EXCLUDED.is_manglik,
    birth_city = EXCLUDED.birth_city,
    gotra = EXCLUDED.gotra
  RETURNING profile_id
),
upsert_preferences AS (
  INSERT INTO partner_preferences (
    profile_id, age_min, age_max, religion, manglik_pref, education_levels,
    occupations, annual_income_min, annual_income_max, height_min_cm,
    height_max_cm, locations, diet_prefs, marital_statuses, family_types,
    relocation_open, timeline, deal_breakers, good_to_have
  )
  SELECT
    persisted_profile_id,
    CASE WHEN gender = 'male' THEN 23 ELSE 27 END,
    CASE WHEN gender = 'male' THEN 32 ELSE 38 END,
    'Hindu',
    'any',
    ARRAY[education_name],
    ARRAY[occupation_name],
    3,
    30,
    CASE WHEN gender = 'male' THEN 150 ELSE 164 END,
    CASE WHEN gender = 'male' THEN 175 ELSE 190 END,
    ARRAY[city_name, state_name],
    ARRAY[diet_value],
    ARRAY['never_married'],
    ARRAY['nuclear','joint'],
    TRUE,
    'within_12_months',
    ARRAY['No hidden expectations'],
    ARRAY['Family oriented']
  FROM profile_rows
  ON CONFLICT (profile_id)
  DO UPDATE SET
    age_min = EXCLUDED.age_min,
    age_max = EXCLUDED.age_max,
    religion = EXCLUDED.religion,
    manglik_pref = EXCLUDED.manglik_pref,
    education_levels = EXCLUDED.education_levels,
    occupations = EXCLUDED.occupations,
    annual_income_min = EXCLUDED.annual_income_min,
    annual_income_max = EXCLUDED.annual_income_max,
    height_min_cm = EXCLUDED.height_min_cm,
    height_max_cm = EXCLUDED.height_max_cm,
    locations = EXCLUDED.locations,
    diet_prefs = EXCLUDED.diet_prefs,
    marital_statuses = EXCLUDED.marital_statuses,
    family_types = EXCLUDED.family_types,
    relocation_open = EXCLUDED.relocation_open,
    timeline = EXCLUDED.timeline,
    deal_breakers = EXCLUDED.deal_breakers,
    good_to_have = EXCLUDED.good_to_have,
    updated_at = NOW()
  RETURNING profile_id
),
upsert_photos AS (
  INSERT INTO profile_photos (
    photo_id, profile_id, photo_url, s3_key, is_primary, is_approved, sequence_order
  )
  SELECT
    photo_id,
    persisted_profile_id,
    image_url,
    'seed/subscription-qa/profile-' || LPAD(i::text, 3, '0') || '/primary.png',
    TRUE,
    TRUE,
    1
  FROM profile_rows
  ON CONFLICT (photo_id)
  DO UPDATE SET
    profile_id = EXCLUDED.profile_id,
    photo_url = EXCLUDED.photo_url,
    s3_key = EXCLUDED.s3_key,
    is_primary = TRUE,
    is_approved = TRUE,
    sequence_order = 1
  RETURNING profile_id
),
updated_subscriptions AS (
  UPDATE subscriptions s
  SET
    plan_id = CASE WHEN pr.subscription_level = 'bronze' THEN 'free' ELSE pr.subscription_level END,
    start_date = NOW() - ((pr.i % 15)::text || ' days')::interval,
    end_date = CASE WHEN pr.subscription_level = 'bronze' THEN NULL ELSE NOW() + INTERVAL '30 days' END,
    is_active = TRUE,
    amount_paid = CASE
      WHEN pr.subscription_level = 'bronze' THEN 0
      WHEN pr.subscription_level = 'silver' THEN 299
      WHEN pr.subscription_level = 'gold' THEN 599
      ELSE 999
    END
  FROM profile_rows pr
  WHERE s.user_id = pr.persisted_user_id
  RETURNING s.user_id
),
inserted_subscriptions AS (
  INSERT INTO subscriptions (
    user_id, plan_id, start_date, end_date, is_active, amount_paid, created_at
  )
  SELECT
    persisted_user_id,
    CASE WHEN subscription_level = 'bronze' THEN 'free' ELSE subscription_level END,
    NOW() - ((i % 15)::text || ' days')::interval,
    CASE WHEN subscription_level = 'bronze' THEN NULL ELSE NOW() + INTERVAL '30 days' END,
    TRUE,
    CASE
      WHEN subscription_level = 'bronze' THEN 0
      WHEN subscription_level = 'silver' THEN 299
      WHEN subscription_level = 'gold' THEN 599
      ELSE 999
    END,
    NOW()
  FROM profile_rows pr
  WHERE NOT EXISTS (
    SELECT 1
    FROM updated_subscriptions us
    WHERE us.user_id = pr.persisted_user_id
  )
  RETURNING user_id
)
INSERT INTO member_subscription_usage (
  user_id, plan_id, period_started_at, period_ends_at,
  profile_views_used, contact_unlocks_used, shortlists_used,
  interests_used, spotlight_boosts_used, updated_at
)
SELECT
  persisted_user_id,
  subscription_level,
  NOW() - INTERVAL '5 days',
  NOW() + INTERVAL '25 days',
  CASE
    WHEN subscription_level = 'bronze' THEN LEAST(i % 11, 10)
    WHEN subscription_level = 'silver' THEN i % 31
    WHEN subscription_level = 'gold' THEN i % 51
    ELSE i % 81
  END,
  CASE
    WHEN subscription_level = 'bronze' THEN 0
    WHEN subscription_level = 'silver' THEN i % 16
    WHEN subscription_level = 'gold' THEN i % 31
    ELSE i % 81
  END,
  CASE
    WHEN subscription_level = 'bronze' THEN i % 6
    WHEN subscription_level = 'silver' THEN i % 21
    WHEN subscription_level = 'gold' THEN i % 41
    ELSE i % 81
  END,
  CASE
    WHEN subscription_level = 'bronze' THEN i % 6
    WHEN subscription_level = 'silver' THEN i % 21
    WHEN subscription_level = 'gold' THEN i % 41
    ELSE i % 81
  END,
  CASE
    WHEN subscription_level = 'gold' THEN i % 3
    WHEN subscription_level = 'platinum' THEN i % 5
    ELSE 0
  END,
  NOW()
FROM profile_rows
ON CONFLICT (user_id)
DO UPDATE SET
  plan_id = EXCLUDED.plan_id,
  period_started_at = EXCLUDED.period_started_at,
  period_ends_at = EXCLUDED.period_ends_at,
  profile_views_used = EXCLUDED.profile_views_used,
  contact_unlocks_used = EXCLUDED.contact_unlocks_used,
  shortlists_used = EXCLUDED.shortlists_used,
  interests_used = EXCLUDED.interests_used,
  spotlight_boosts_used = EXCLUDED.spotlight_boosts_used,
  updated_at = NOW();

CREATE OR REPLACE VIEW qa_subscription_seed_profiles AS
SELECT
  p.profile_id AS id,
  CONCAT_WS(' ', p.first_name, p.last_name) AS name,
  p.gender,
  EXTRACT(YEAR FROM AGE(CURRENT_DATE, p.dob))::int AS age,
  fd.family_state AS state,
  p.mother_tongue AS language,
  msu.plan_id AS subscription_level,
  p.primary_photo_url AS image_url,
  (p.contact_privacy = 'masked') AS masked_contact,
  msu.contact_unlocks_used AS unmask_count,
  p.photo_privacy,
  p.profile_created_by,
  p.verification_status
FROM profiles p
JOIN family_details fd ON fd.profile_id = p.profile_id
JOIN member_subscription_usage msu ON msu.user_id = p.user_id
WHERE p.profile_id BETWEEN '72000000-0000-4000-8000-000000000001'::uuid
                       AND '72000000-0000-4000-8000-000000000100'::uuid
ORDER BY p.gender DESC, p.profile_id;
