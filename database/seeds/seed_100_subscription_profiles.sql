-- QA seed for subscription-limit testing. Run manually against non-production databases.
-- Creates 100 diverse member profiles (50 male, 50 female) with generated portrait URLs.

WITH seed AS (
  SELECT
    i,
    CASE WHEN i <= 50 THEN 'male' ELSE 'female' END AS gender,
    (ARRAY['Maharashtra','Tamil Nadu','Karnataka','Delhi','Gujarat','Kerala','Punjab','Telangana','West Bengal','Rajasthan'])[((i - 1) % 10) + 1] AS state_name,
    (ARRAY['Hindi','Tamil','Telugu','Kannada','Marathi','Bengali','Punjabi','Gujarati','Malayalam'])[((i - 1) % 9) + 1] AS language_name,
    (ARRAY['Aarav','Vivaan','Aditya','Arjun','Reyansh','Kabir','Rohan','Karthik','Rahul','Siddharth'])[((i - 1) % 10) + 1] AS male_first_name,
    (ARRAY['Aadhya','Ananya','Diya','Isha','Kavya','Meera','Nisha','Priya','Saanvi','Veda'])[((i - 1) % 10) + 1] AS female_first_name,
    (ARRAY['Sharma','Reddy','Iyer','Patel','Nair','Singh','Gupta','Das','Kulkarni','Menon'])[((i - 1) % 10) + 1] AS last_name
  FROM generate_series(1, 100) AS i
),
inserted_users AS (
  INSERT INTO users (phone, email, is_verified, user_type, role_selected_at)
  SELECT
    '+9199' || LPAD(i::text, 8, '0'),
    'qa.member.' || i || '@soulmatch.local',
    TRUE,
    'member',
    NOW()
  FROM seed
  ON CONFLICT (phone) DO UPDATE SET updated_at=NOW()
  RETURNING user_id, phone, email
),
user_rows AS (
  SELECT
    s.*,
    u.user_id,
    u.phone,
    u.email
  FROM seed s
  JOIN inserted_users u ON u.email = 'qa.member.' || s.i || '@soulmatch.local'
),
inserted_profiles AS (
  INSERT INTO profiles (
    user_id, first_name, last_name, dob, gender, religion, caste, mother_tongue,
    marital_status, completion_score, is_published, profile_status,
    profile_created_by, verification_status, admin_status, primary_photo_url,
    photo_privacy, contact_privacy, profile_visibility
  )
  SELECT
    user_id,
    CASE WHEN gender='male' THEN male_first_name ELSE female_first_name END,
    last_name,
    (CURRENT_DATE - ((23 + (i % 15))::text || ' years')::interval)::date,
    gender,
    'Hindu',
    CASE WHEN i % 3 = 0 THEN 'Brahmin' WHEN i % 3 = 1 THEN 'Reddy' ELSE 'Vaishya' END,
    language_name,
    'never_married',
    100,
    TRUE,
    'active',
    CASE WHEN i % 5 = 0 THEN 'mediator' ELSE 'self' END,
    CASE WHEN i % 4 = 0 THEN 'verified' ELSE 'pending' END,
    'active',
    'https://api.dicebear.com/8.x/personas/png?seed=soulmatch-' || gender || '-' || i,
    CASE WHEN i % 7 = 0 THEN 'request_only' ELSE 'all' END,
    CASE WHEN i % 6 = 0 THEN 'masked' ELSE 'visible' END,
    'all'
  FROM user_rows
  ON CONFLICT DO NOTHING
  RETURNING profile_id, user_id
)
INSERT INTO member_subscription_usage (user_id, plan_id, period_started_at, period_ends_at)
SELECT
  user_id,
  CASE
    WHEN ROW_NUMBER() OVER (ORDER BY user_id) % 4 = 0 THEN 'platinum'
    WHEN ROW_NUMBER() OVER (ORDER BY user_id) % 4 = 1 THEN 'bronze'
    WHEN ROW_NUMBER() OVER (ORDER BY user_id) % 4 = 2 THEN 'silver'
    ELSE 'gold'
  END,
  NOW(),
  NOW() + INTERVAL '30 days'
FROM inserted_profiles
ON CONFLICT (user_id) DO NOTHING;
