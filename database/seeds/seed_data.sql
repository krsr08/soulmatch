INSERT INTO users (user_id, phone, is_verified, is_active)
VALUES
    ('11111111-1111-1111-1111-111111111111', '+919876543210', TRUE, TRUE),
    ('22222222-2222-2222-2222-222222222222', '+919876543211', TRUE, TRUE),
    ('33333333-3333-3333-3333-333333333333', '+919876543212', TRUE, TRUE)
ON CONFLICT DO NOTHING;

INSERT INTO subscriptions (user_id, plan_id, is_active)
VALUES
    ('11111111-1111-1111-1111-111111111111', 'free', TRUE),
    ('22222222-2222-2222-2222-222222222222', 'gold', TRUE),
    ('33333333-3333-3333-3333-333333333333', 'free', TRUE)
ON CONFLICT DO NOTHING;

INSERT INTO profiles (
    profile_id,
    user_id,
    first_name,
    last_name,
    dob,
    gender,
    religion,
    caste,
    mother_tongue,
    marital_status,
    completion_score,
    is_published,
    verification_status,
    admin_status,
    primary_photo_url
)
VALUES
    (
        'aaaa0001-aaaa-aaaa-aaaa-aaaaaaaaaaaa',
        '11111111-1111-1111-1111-111111111111',
        'Rahul',
        'Sharma',
        '1995-05-15',
        'male',
        'Hindu',
        'Brahmin',
        'Hindi',
        'never_married',
        85,
        TRUE,
        'verified',
        'active',
        'https://images.unsplash.com/photo-1500648767791-00dcc994a43e?auto=format&fit=crop&w=900&q=80'
    ),
    (
        'aaaa0002-aaaa-aaaa-aaaa-aaaaaaaaaaaa',
        '22222222-2222-2222-2222-222222222222',
        'Priya',
        'Gupta',
        '1997-08-22',
        'female',
        'Hindu',
        'Vaishya',
        'Hindi',
        'never_married',
        90,
        TRUE,
        'verified',
        'active',
        'https://images.unsplash.com/photo-1494790108377-be9c29b29330?auto=format&fit=crop&w=900&q=80'
    ),
    (
        'aaaa0003-aaaa-aaaa-aaaa-aaaaaaaaaaaa',
        '33333333-3333-3333-3333-333333333333',
        'Amit',
        'Patel',
        '1993-03-10',
        'male',
        'Hindu',
        'Patel',
        'Gujarati',
        'never_married',
        75,
        TRUE,
        'verified',
        'active',
        'https://images.unsplash.com/photo-1506794778202-cad84cf45f1d?auto=format&fit=crop&w=900&q=80'
    )
ON CONFLICT DO NOTHING;

INSERT INTO education_career (profile_id, education_level, occupation, annual_income, working_city)
VALUES
    ('aaaa0001-aaaa-aaaa-aaaa-aaaaaaaaaaaa', 'Post Graduate', 'Software Engineer', '10-20 LPA', 'Bangalore'),
    ('aaaa0002-aaaa-aaaa-aaaa-aaaaaaaaaaaa', 'Graduate', 'Doctor', '10-20 LPA', 'Mumbai'),
    ('aaaa0003-aaaa-aaaa-aaaa-aaaaaaaaaaaa', 'Graduate', 'Business', '5-10 LPA', 'Ahmedabad')
ON CONFLICT DO NOTHING;

INSERT INTO physical_details (profile_id, height_cm, weight_kg, complexion, body_type, blood_group)
VALUES
    ('aaaa0001-aaaa-aaaa-aaaa-aaaaaaaaaaaa', 175, 70, 'Fair', 'Average', 'O+'),
    ('aaaa0002-aaaa-aaaa-aaaa-aaaaaaaaaaaa', 162, 55, 'Fair', 'Slim', 'A+'),
    ('aaaa0003-aaaa-aaaa-aaaa-aaaaaaaaaaaa', 170, 75, 'Wheatish', 'Average', 'B+')
ON CONFLICT DO NOTHING;

INSERT INTO lifestyle_details (profile_id, diet, smoking, drinking, about_me)
VALUES
    ('aaaa0001-aaaa-aaaa-aaaa-aaaaaaaaaaaa', 'vegetarian', 'never', 'never', 'Software engineer passionate about technology.'),
    ('aaaa0002-aaaa-aaaa-aaaa-aaaaaaaaaaaa', 'vegetarian', 'never', 'never', 'Medical professional dedicated to helping others.'),
    ('aaaa0003-aaaa-aaaa-aaaa-aaaaaaaaaaaa', 'non_vegetarian', 'never', 'occasionally', 'Business owner with love for cricket.')
ON CONFLICT DO NOTHING;

INSERT INTO landing_pages (
    slug,
    title,
    subtitle,
    description,
    hero_image_url,
    preview_image_url,
    cta_label,
    cta_url,
    seo_title,
    seo_description,
    is_active
)
VALUES
    (
        'home',
        'SoulMatch for serious families',
        'Verified profiles, premium discovery, and a thoughtful path to marriage.',
        'SoulMatch brings together trusted matrimonial discovery, elegant storytelling, and premium relationship signals for families who want clarity.',
        'https://images.unsplash.com/photo-1519741497674-611481863552?auto=format&fit=crop&w=1600&q=80',
        'https://images.unsplash.com/photo-1519741497674-611481863552?auto=format&fit=crop&w=1200&q=80',
        'Open app',
        'https://app.soulmatch.app',
        'SoulMatch | Verified matrimonial discovery',
        'Share a polished SoulMatch landing page with dynamic branding and campaign attribution ready for WhatsApp and LinkedIn.',
        TRUE
    )
ON CONFLICT (slug) DO NOTHING;

INSERT INTO referral_codes (
    code,
    owner_user_id,
    campaign_name,
    channel,
    reward_points,
    max_redemptions,
    is_active
)
VALUES
    (
        'SOULMATCHVIP',
        '22222222-2222-2222-2222-222222222222',
        'Founders Circle',
        'organic',
        100,
        1000,
        TRUE
    )
ON CONFLICT (code) DO NOTHING;
