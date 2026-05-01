DO $$
DECLARE
    target_total INTEGER := 500;
    current_total INTEGER := 0;
    missing_count INTEGER := 0;
    i INTEGER;
    seed_no INTEGER;
    profile_user_id UUID;
    new_profile_id UUID;
    gender_value TEXT;
    first_names TEXT[];
    last_names TEXT[];
    castes TEXT[];
    cities TEXT[];
    tongue TEXT;
    religion_value TEXT := 'Hindu';
    first_name_value TEXT;
    last_name_value TEXT;
    caste_value TEXT;
    city_value TEXT;
    occupation_value TEXT;
    education_value TEXT;
    income_value TEXT;
    family_type_value TEXT;
    diet_value TEXT;
    height_value INTEGER;
    birth_date DATE;
    verification_value TEXT;
    photo_value TEXT;
    about_value TEXT;
    phone_value TEXT;
    email_value TEXT;
    occupations TEXT[] := ARRAY[
        'Software Engineer','Product Manager','Doctor','Chartered Accountant','Teacher','Business Analyst',
        'Civil Engineer','Data Analyst','Bank Manager','Marketing Lead','Pharmacist','Architect',
        'Government Officer','HR Manager','UX Designer','Entrepreneur','Professor','Lawyer'
    ];
    educations TEXT[] := ARRAY['B.Tech','M.Tech','MBA','MBBS','B.Com','M.Com','CA','B.Sc','M.Sc','MA','B.Arch','LLB'];
    incomes TEXT[] := ARRAY['6-8 LPA','8-12 LPA','12-18 LPA','18-25 LPA','25-35 LPA','35 LPA+'];
    family_types TEXT[] := ARRAY['nuclear','joint','upper_middle_class','traditional','moderate'];
    diets TEXT[] := ARRAY['vegetarian','non_vegetarian','eggetarian','jain'];
BEGIN
    SELECT COUNT(*) INTO current_total FROM profiles;
    missing_count := GREATEST(0, target_total - current_total);

    FOR i IN 1..missing_count LOOP
        seed_no := current_total + i;
        gender_value := CASE WHEN seed_no % 2 = 0 THEN 'female' ELSE 'male' END;

        CASE (seed_no - 1) % 8
            WHEN 0 THEN
                tongue := 'Telugu';
                castes := ARRAY['Reddy','Kamma','Kapu','Velama','Brahmin','Balija'];
                cities := ARRAY['Hyderabad','Vijayawada','Visakhapatnam','Guntur','Warangal','Tirupati'];
                last_names := ARRAY['Reddy','Naidu','Rao','Chowdary','Varma','Sastry'];
                first_names := CASE WHEN gender_value='female'
                    THEN ARRAY['Anusha','Sravani','Madhavi','Harika','Deepthi','Lasya','Nandini','Keerthi']
                    ELSE ARRAY['Arjun','Karthik','Sai Teja','Rohit','Vamsi','Nikhil','Sandeep','Praneeth'] END;
            WHEN 1 THEN
                tongue := 'Tamil';
                castes := ARRAY['Mudaliar','Gounder','Vanniyar','Iyer','Nadar','Chettiar'];
                cities := ARRAY['Chennai','Coimbatore','Madurai','Trichy','Salem','Erode'];
                last_names := ARRAY['Subramanian','Raman','Krishnan','Iyer','Natarajan','Balasubramanian'];
                first_names := CASE WHEN gender_value='female'
                    THEN ARRAY['Meenakshi','Kavya','Aishwarya','Divya','Nivetha','Priyanka','Janani','Shruthi']
                    ELSE ARRAY['Vignesh','Sriram','Aravind','Karthikeyan','Prakash','Vijay','Surya','Madhan'] END;
            WHEN 2 THEN
                tongue := 'Malayalam';
                castes := ARRAY['Nair','Ezhava','Syrian Christian','Menon','Namboodiri','Thiyya'];
                cities := ARRAY['Kochi','Thiruvananthapuram','Kozhikode','Thrissur','Kottayam','Palakkad'];
                last_names := ARRAY['Nair','Menon','Pillai','Varghese','Thomas','Kurian'];
                first_names := CASE WHEN gender_value='female'
                    THEN ARRAY['Anjali','Aparna','Devika','Lakshmi','Malavika','Nithya','Parvathy','Reshma']
                    ELSE ARRAY['Arun','Vishnu','Rahul','Naveen','Jithin','Aditya','Akhil','Vivek'] END;
            WHEN 3 THEN
                tongue := 'Hindi';
                castes := ARRAY['Agarwal','Brahmin','Kayastha','Rajput','Baniya','Yadav'];
                cities := ARRAY['Delhi','Lucknow','Jaipur','Noida','Gurugram','Bhopal'];
                last_names := ARRAY['Sharma','Gupta','Singh','Agarwal','Verma','Tiwari'];
                first_names := CASE WHEN gender_value='female'
                    THEN ARRAY['Ananya','Riya','Neha','Pooja','Isha','Sakshi','Kriti','Naina']
                    ELSE ARRAY['Aarav','Rohan','Aditya','Kunal','Siddharth','Abhishek','Nitin','Varun'] END;
            WHEN 4 THEN
                tongue := 'Punjabi';
                castes := ARRAY['Khatri','Arora','Jat Sikh','Ramgarhia','Brahmin','Saini'];
                cities := ARRAY['Chandigarh','Ludhiana','Amritsar','Jalandhar','Patiala','Delhi'];
                last_names := ARRAY['Kaur','Singh','Malhotra','Bedi','Sethi','Chawla'];
                first_names := CASE WHEN gender_value='female'
                    THEN ARRAY['Simran','Harpreet','Gurleen','Jasleen','Navneet','Amandeep','Mehak','Prabhjot']
                    ELSE ARRAY['Gurpreet','Manpreet','Harshdeep','Armaan','Jaspreet','Kabir','Amrit','Tejinder'] END;
            WHEN 5 THEN
                tongue := 'Gujarati';
                castes := ARRAY['Patel','Vaishnav','Brahmin','Lohana','Jain','Modh Baniya'];
                cities := ARRAY['Ahmedabad','Surat','Vadodara','Rajkot','Gandhinagar','Bhavnagar'];
                last_names := ARRAY['Patel','Shah','Mehta','Desai','Trivedi','Joshi'];
                first_names := CASE WHEN gender_value='female'
                    THEN ARRAY['Hetal','Krupa','Mansi','Dhara','Nirali','Riddhi','Jinal','Forum']
                    ELSE ARRAY['Harsh','Parth','Meet','Bhavik','Darshan','Nirav','Krunal','Yash'] END;
            WHEN 6 THEN
                tongue := 'Odia';
                castes := ARRAY['Karana','Brahmin','Khandayat','Chasa','Kayastha','Teli'];
                cities := ARRAY['Bhubaneswar','Cuttack','Rourkela','Puri','Sambalpur','Balasore'];
                last_names := ARRAY['Pattnaik','Mishra','Das','Mohanty','Sahoo','Nayak'];
                first_names := CASE WHEN gender_value='female'
                    THEN ARRAY['Lopamudra','Suchismita','Ipsita','Ankita','Priyadarshini','Madhusmita','Subhashree','Rashmi']
                    ELSE ARRAY['Sambit','Anubhav','Debasis','Satyajit','Pratyush','Abinash','Soumya','Rakesh'] END;
            ELSE
                tongue := 'Marathi';
                castes := ARRAY['Maratha','Deshastha Brahmin','CKP','Kokanastha','Mali','Kunbi'];
                cities := ARRAY['Mumbai','Pune','Nagpur','Nashik','Kolhapur','Thane'];
                last_names := ARRAY['Deshmukh','Patil','Joshi','Kulkarni','More','Gokhale'];
                first_names := CASE WHEN gender_value='female'
                    THEN ARRAY['Aditi','Prajakta','Sneha','Mrunal','Vaishnavi','Rutuja','Pallavi','Sayali']
                    ELSE ARRAY['Omkar','Saurabh','Akshay','Mandar','Pratik','Rohit','Sanket','Nikhil'] END;
        END CASE;

        first_name_value := first_names[((seed_no - 1) % array_length(first_names, 1)) + 1];
        last_name_value := last_names[((seed_no - 1) % array_length(last_names, 1)) + 1];
        caste_value := castes[((seed_no - 1) % array_length(castes, 1)) + 1];
        city_value := cities[((seed_no - 1) % array_length(cities, 1)) + 1];
        occupation_value := occupations[((seed_no - 1) % array_length(occupations, 1)) + 1];
        education_value := educations[((seed_no - 1) % array_length(educations, 1)) + 1];
        income_value := incomes[((seed_no - 1) % array_length(incomes, 1)) + 1];
        family_type_value := family_types[((seed_no - 1) % array_length(family_types, 1)) + 1];
        diet_value := diets[((seed_no - 1) % array_length(diets, 1)) + 1];
        height_value := CASE WHEN gender_value='female' THEN 152 + (seed_no % 24) ELSE 164 + (seed_no % 26) END;
        birth_date := CURRENT_DATE - ((24 + (seed_no % 12)) * INTERVAL '1 year') - ((seed_no % 365) * INTERVAL '1 day');
        verification_value := 'verified';
        photo_value := CASE WHEN gender_value='female'
            THEN 'https://api.dicebear.com/9.x/personas/png?seed=' || tongue || '-bride-' || seed_no || '&backgroundColor=fff0f3,fef3c7,e0f2fe'
            ELSE 'https://api.dicebear.com/9.x/personas/png?seed=' || tongue || '-groom-' || seed_no || '&backgroundColor=e0f2fe,f3e8ff,fef3c7'
        END;
        about_value := first_name_value || ' is a ' || education_value || ' graduate working as a ' || occupation_value ||
            ' in ' || city_value || '. The family values education, respect, and a thoughtful marriage journey.';
        phone_value := '97' || LPAD(seed_no::TEXT, 8, '0');
        email_value := 'soulmatch.seed.' || seed_no || '@example.test';

        INSERT INTO users (phone, email, is_verified, is_active, acquisition_source, last_login, created_at, updated_at)
        VALUES (phone_value, email_value, verification_value='verified', true, 'seed_500_profiles', NOW() - ((seed_no % 30) * INTERVAL '1 day'), NOW(), NOW())
        ON CONFLICT (phone) DO UPDATE SET updated_at=NOW()
        RETURNING user_id INTO profile_user_id;

        INSERT INTO profiles (
            user_id, first_name, last_name, dob, gender, religion, caste, mother_tongue,
            marital_status, completion_score, is_published, verification_status, admin_status,
            primary_photo_url, photo_privacy, profile_visibility, created_at, updated_at
        )
        VALUES (
            profile_user_id, first_name_value, last_name_value, birth_date, gender_value, religion_value, caste_value, tongue,
            'never_married', 88 + (seed_no % 12), true, verification_value, 'active',
            photo_value, 'all', 'all', NOW() - ((seed_no % 90) * INTERVAL '1 day'), NOW()
        )
        RETURNING profile_id INTO new_profile_id;

        INSERT INTO physical_details (profile_id, height_cm, weight_kg, complexion, body_type, blood_group)
        VALUES (new_profile_id, height_value, CASE WHEN gender_value='female' THEN 48 + (seed_no % 18) ELSE 62 + (seed_no % 24) END, 'wheatish', 'average', 'O+');

        INSERT INTO education_career (profile_id, education_level, occupation, annual_income, working_city)
        VALUES (new_profile_id, education_value, occupation_value, income_value, city_value);

        INSERT INTO family_details (profile_id, father_occupation, mother_occupation, num_brothers, num_sisters, family_type, family_city)
        VALUES (new_profile_id, 'Business / Service', 'Homemaker', seed_no % 3, (seed_no + 1) % 3, family_type_value, city_value);

        INSERT INTO horoscope_details (profile_id, rashi, nakshatra, is_manglik, birth_city, gotra)
        VALUES (new_profile_id, (ARRAY['Mesha','Vrishabha','Mithuna','Karka','Simha','Kanya'])[(seed_no % 6) + 1], (ARRAY['Ashwini','Rohini','Mrigashira','Pushya','Magha','Hasta'])[(seed_no % 6) + 1], seed_no % 7 = 0, city_value, caste_value);

        INSERT INTO lifestyle_details (profile_id, diet, smoking, drinking, about_me)
        VALUES (new_profile_id, diet_value, 'never', 'never', about_value);

        INSERT INTO partner_preferences (profile_id, age_min, age_max, religion, manglik_pref)
        VALUES (new_profile_id, 23, 34, religion_value, 'any');

        INSERT INTO subscriptions (user_id, plan_id, is_active, amount_paid)
        VALUES (profile_user_id, 'free', true, 0);

        INSERT INTO profile_photos (profile_id, photo_url, s3_key, is_primary, is_approved, sequence_order)
        VALUES (new_profile_id, photo_value, 'seed/profile-' || seed_no || '/primary.png', true, true, 1);
    END LOOP;
END $$;
