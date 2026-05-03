package com.soulmatch.app.data.mock

import com.soulmatch.app.data.models.ChatLastMessage
import com.soulmatch.app.data.models.ChatMessage
import com.soulmatch.app.data.models.BotFlowMessage
import com.soulmatch.app.data.models.CompatibilityBreakdown
import com.soulmatch.app.data.models.CompatibilityData
import com.soulmatch.app.data.models.ConversationItem
import com.soulmatch.app.data.models.InterestListItem
import com.soulmatch.app.data.models.InvoiceItem
import com.soulmatch.app.data.models.PlanData
import com.soulmatch.app.data.models.ProfileData
import com.soulmatch.app.data.models.ProfilePhoto
import com.soulmatch.app.data.models.ProfileSummary
import com.soulmatch.app.data.models.SavedSearchData
import com.soulmatch.app.data.models.SearchRequest
import com.soulmatch.app.data.models.ShortlistItem
import com.soulmatch.app.data.models.SubscriptionData
import com.soulmatch.app.data.models.ViewerData

object MarketFixtures {
    const val currentUserId = "11111111-1111-1111-1111-111111111111"
    const val currentProfileId = "aaaa0001-aaaa-aaaa-aaaa-aaaaaaaaaaaa"

    private const val priyaUserId = "22222222-2222-2222-2222-222222222222"
    private const val priyaProfileId = "aaaa0002-aaaa-aaaa-aaaa-aaaaaaaaaaaa"
    private const val nishaUserId = "44444444-4444-4444-4444-444444444444"
    private const val nishaProfileId = "aaaa0004-aaaa-aaaa-aaaa-aaaaaaaaaaaa"
    private const val ananyaUserId = "55555555-5555-5555-5555-555555555555"
    private const val ananyaProfileId = "aaaa0005-aaaa-aaaa-aaaa-aaaaaaaaaaaa"
    private const val kavyaUserId = "66666666-6666-6666-6666-666666666666"
    private const val kavyaProfileId = "aaaa0006-aaaa-aaaa-aaaa-aaaaaaaaaaaa"

    var myProfile = ProfileData(
        profileId = currentProfileId,
        userId = currentUserId,
        firstName = "Aarav",
        lastName = "Sharma",
        dob = "1995-05-15",
        age = 30,
        gender = "male",
        religion = "Hindu",
        caste = "Brahmin",
        motherTongue = "Hindi",
        maritalStatus = "never_married",
        completionScore = 85,
        primaryPhotoUrl = "https://i.pravatar.cc/300?img=12",
        educationLevel = "Post Graduate",
        occupation = "Software Engineer",
        annualIncome = "18-24 LPA",
        workingCity = "Bengaluru",
        heightCm = 175,
        weightKg = 70,
        complexion = "Fair",
        bodyType = "Athletic",
        bloodGroup = "O+",
        fatherOccupation = "Retired banker",
        motherOccupation = "Teacher",
        numBrothers = 1,
        numSisters = 0,
        familyType = "Nuclear",
        familyCity = "Jaipur",
        diet = "vegetarian",
        smoking = "never",
        drinking = "occasionally",
        aboutMe = "Product-minded engineer who values family, curiosity, and a calm home life.",
        rashi = "Mithun",
        nakshatra = "Ardra",
        isManglik = false,
        birthCity = "Jaipur",
        gotra = "Bharadwaj"
    )
        private set

    private val detailedProfiles = listOf(
        ProfileData(
            profileId = priyaProfileId,
            userId = priyaUserId,
            firstName = "Priya",
            lastName = "Gupta",
            dob = "1997-08-22",
            age = 28,
            gender = "female",
            religion = "Hindu",
            caste = "Vaishya",
            motherTongue = "Hindi",
            maritalStatus = "never_married",
            completionScore = 94,
            primaryPhotoUrl = "https://i.pravatar.cc/300?img=47",
            educationLevel = "Graduate",
            occupation = "Doctor",
            annualIncome = "15-20 LPA",
            workingCity = "Mumbai",
            heightCm = 162,
            weightKg = 55,
            complexion = "Fair",
            bodyType = "Slim",
            bloodGroup = "A+",
            fatherOccupation = "Business",
            motherOccupation = "Homemaker",
            numBrothers = 1,
            numSisters = 1,
            familyType = "Joint",
            familyCity = "Mumbai",
            diet = "vegetarian",
            smoking = "never",
            drinking = "never",
            aboutMe = "Doctor who loves classical music, weekend travel, and thoughtful conversations.",
            rashi = "Kanya",
            nakshatra = "Hasta",
            isManglik = false,
            birthCity = "Mumbai",
            gotra = "Kashyap"
        ),
        ProfileData(
            profileId = nishaProfileId,
            userId = nishaUserId,
            firstName = "Nisha",
            lastName = "Iyer",
            dob = "1996-11-09",
            age = 29,
            gender = "female",
            religion = "Hindu",
            caste = "Iyer",
            motherTongue = "Tamil",
            maritalStatus = "never_married",
            completionScore = 89,
            primaryPhotoUrl = "https://i.pravatar.cc/300?img=36",
            photoPrivacy = "matches_only",
            educationLevel = "Post Graduate",
            occupation = "Product Manager",
            annualIncome = "20-30 LPA",
            workingCity = "Chennai",
            heightCm = 167,
            weightKg = 58,
            complexion = "Wheatish",
            bodyType = "Average",
            bloodGroup = "B+",
            fatherOccupation = "Professor",
            motherOccupation = "Doctor",
            numBrothers = 0,
            numSisters = 1,
            familyType = "Nuclear",
            familyCity = "Chennai",
            diet = "vegetarian",
            smoking = "never",
            drinking = "socially",
            aboutMe = "I enjoy building thoughtful products, temple town weekends, and coffee that is stronger than my calendar.",
            rashi = "Vrischik",
            nakshatra = "Anuradha",
            isManglik = false,
            birthCity = "Chennai",
            gotra = "Harita"
        ),
        ProfileData(
            profileId = ananyaProfileId,
            userId = ananyaUserId,
            firstName = "Ananya",
            lastName = "Desai",
            dob = "1998-03-17",
            age = 27,
            gender = "female",
            religion = "Hindu",
            caste = "Gujarati",
            motherTongue = "Gujarati",
            maritalStatus = "never_married",
            completionScore = 91,
            primaryPhotoUrl = "https://i.pravatar.cc/300?img=25",
            educationLevel = "MBA",
            occupation = "Founder",
            annualIncome = "25-35 LPA",
            workingCity = "Ahmedabad",
            heightCm = 165,
            weightKg = 54,
            complexion = "Fair",
            bodyType = "Athletic",
            bloodGroup = "O+",
            fatherOccupation = "Entrepreneur",
            motherOccupation = "Homemaker",
            numBrothers = 1,
            numSisters = 0,
            familyType = "Joint",
            familyCity = "Ahmedabad",
            diet = "jain",
            smoking = "never",
            drinking = "never",
            aboutMe = "Running a wellness brand, close to family, and serious about building a grounded partnership.",
            rashi = "Meen",
            nakshatra = "Revati",
            isManglik = true,
            birthCity = "Ahmedabad",
            gotra = "Vatsa"
        ),
        ProfileData(
            profileId = kavyaProfileId,
            userId = kavyaUserId,
            firstName = "Kavya",
            lastName = "Reddy",
            dob = "1995-12-04",
            age = 30,
            gender = "female",
            religion = "Hindu",
            caste = "Reddy",
            motherTongue = "Telugu",
            maritalStatus = "never_married",
            completionScore = 87,
            primaryPhotoUrl = "https://i.pravatar.cc/300?img=15",
            educationLevel = "Graduate",
            occupation = "Architect",
            annualIncome = "12-18 LPA",
            workingCity = "Hyderabad",
            heightCm = 169,
            weightKg = 60,
            complexion = "Wheatish",
            bodyType = "Average",
            bloodGroup = "AB+",
            fatherOccupation = "Civil contractor",
            motherOccupation = "Teacher",
            numBrothers = 0,
            numSisters = 1,
            familyType = "Nuclear",
            familyCity = "Hyderabad",
            diet = "eggetarian",
            smoking = "never",
            drinking = "socially",
            aboutMe = "Architect with a love for design-led travel, long drives, and family get-togethers.",
            rashi = "Dhanu",
            nakshatra = "Moola",
            isManglik = false,
            birthCity = "Hyderabad",
            gotra = "Atri"
        )
    )

    private data class RegionalProfileSeed(
        val motherTongue: String,
        val femaleNames: List<String>,
        val maleNames: List<String>,
        val lastNames: List<String>,
        val communities: List<String>,
        val cities: List<String>
    )

    private val generatedRegions = listOf(
        RegionalProfileSeed(
            motherTongue = "Telugu",
            femaleNames = listOf("Sravya", "Anusha", "Harika", "Mounika", "Pranavi", "Lasya", "Bhavya", "Deepthi"),
            maleNames = listOf("Sai Kiran", "Vamsi", "Naveen", "Karthik", "Abhinav", "Rohit", "Sandeep", "Charan"),
            lastNames = listOf("Reddy", "Naidu", "Rao", "Chowdary", "Varma", "Goud"),
            communities = listOf("Reddy", "Kamma", "Naidu", "Kapu", "Brahmin", "Goud"),
            cities = listOf("Hyderabad", "Vijayawada", "Visakhapatnam", "Warangal", "Guntur", "Tirupati")
        ),
        RegionalProfileSeed(
            motherTongue = "Tamil",
            femaleNames = listOf("Nandhini", "Priyanka", "Keerthana", "Aishwarya", "Subiksha", "Janani", "Divya", "Lakshmi"),
            maleNames = listOf("Aravind", "Karthikeyan", "Pradeep", "Vignesh", "Sriram", "Balaji", "Gokul", "Vikram"),
            lastNames = listOf("Iyer", "Iyengar", "Pillai", "Raman", "Subramanian", "Krishnan"),
            communities = listOf("Iyer", "Iyengar", "Mudaliar", "Pillai", "Chettiar", "Gounder"),
            cities = listOf("Chennai", "Coimbatore", "Madurai", "Trichy", "Salem", "Erode")
        ),
        RegionalProfileSeed(
            motherTongue = "Malayalam",
            femaleNames = listOf("Anjali", "Aparna", "Meera", "Devika", "Malavika", "Parvathy", "Nimisha", "Athira"),
            maleNames = listOf("Arun", "Nikhil", "Vishnu", "Rahul", "Sreejith", "Akhil", "Anand", "Rakesh"),
            lastNames = listOf("Nair", "Menon", "Pillai", "Warrier", "Kurian", "Mathew"),
            communities = listOf("Nair", "Menon", "Ezhava", "Syrian Christian", "Namboothiri", "Pillai"),
            cities = listOf("Kochi", "Thiruvananthapuram", "Kozhikode", "Thrissur", "Kottayam", "Alappuzha")
        ),
        RegionalProfileSeed(
            motherTongue = "Hindi",
            femaleNames = listOf("Aditi", "Radhika", "Shreya", "Nikita", "Ishita", "Sakshi", "Kritika", "Pooja"),
            maleNames = listOf("Rohan", "Amit", "Nikhil", "Aditya", "Siddharth", "Yash", "Dhruv", "Kunal"),
            lastNames = listOf("Sharma", "Gupta", "Agarwal", "Bansal", "Tiwari", "Saxena"),
            communities = listOf("Brahmin", "Agarwal", "Kayastha", "Rajput", "Vaishya", "Baniya"),
            cities = listOf("Delhi", "Gurugram", "Noida", "Lucknow", "Jaipur", "Bhopal")
        ),
        RegionalProfileSeed(
            motherTongue = "Punjabi",
            femaleNames = listOf("Simran", "Jasleen", "Harleen", "Gurpreet", "Navneet", "Mehak", "Ishleen", "Rupinder"),
            maleNames = listOf("Armaan", "Harpreet", "Gurkirat", "Manpreet", "Jaspreet", "Kabir", "Ranveer", "Karan"),
            lastNames = listOf("Singh", "Kaur", "Gill", "Bedi", "Sodhi", "Chawla"),
            communities = listOf("Punjabi", "Khatri", "Arora", "Jat Sikh", "Ramgarhia", "Brahmin"),
            cities = listOf("Chandigarh", "Ludhiana", "Amritsar", "Jalandhar", "Patiala", "Delhi")
        ),
        RegionalProfileSeed(
            motherTongue = "Gujarati",
            femaleNames = listOf("Ananya", "Hetal", "Krupa", "Niyati", "Riddhi", "Jinal", "Mansi", "Dhara"),
            maleNames = listOf("Harsh", "Dhruv", "Mihir", "Kunal", "Parth", "Nirav", "Yash", "Jay"),
            lastNames = listOf("Patel", "Shah", "Mehta", "Desai", "Trivedi", "Joshi"),
            communities = listOf("Patel", "Vaishnav", "Gujarati", "Jain", "Lohana", "Brahmin"),
            cities = listOf("Ahmedabad", "Surat", "Vadodara", "Rajkot", "Gandhinagar", "Mumbai")
        ),
        RegionalProfileSeed(
            motherTongue = "Odia",
            femaleNames = listOf("Ipsita", "Suchismita", "Anwesha", "Priyadarshini", "Madhusmita", "Lopamudra", "Rashmita", "Debasmita"),
            maleNames = listOf("Sabyasachi", "Abhishek", "Debasis", "Pratik", "Amlan", "Subham", "Sourav", "Ritesh"),
            lastNames = listOf("Dash", "Panda", "Mishra", "Mohanty", "Sahoo", "Patnaik"),
            communities = listOf("Karana", "Brahmin", "Khandayat", "Panda", "Mohanty", "Sahoo"),
            cities = listOf("Bhubaneswar", "Cuttack", "Rourkela", "Puri", "Sambalpur", "Balasore")
        ),
        RegionalProfileSeed(
            motherTongue = "Marathi",
            femaleNames = listOf("Mrunal", "Vaidehi", "Aarohi", "Sayali", "Neha", "Prajakta", "Gauri", "Rutuja"),
            maleNames = listOf("Omkar", "Saurabh", "Akshay", "Nikhil", "Tejas", "Pranav", "Aditya", "Rohit"),
            lastNames = listOf("Deshmukh", "Kulkarni", "Joshi", "Patil", "Shinde", "Gokhale"),
            communities = listOf("Maratha", "Brahmin", "CKP", "Kunbi", "Deshastha", "Kokanastha"),
            cities = listOf("Mumbai", "Pune", "Nagpur", "Nashik", "Kolhapur", "Thane")
        )
    )

    private val generatedProfileImages = listOf(
        "android.resource://com.soulmatch.app/drawable/login_region_south_kanjeevaram",
        "android.resource://com.soulmatch.app/drawable/login_region_south_kasavu",
        "android.resource://com.soulmatch.app/drawable/login_region_north_banarasi",
        "android.resource://com.soulmatch.app/drawable/login_region_north_punjabi",
        "android.resource://com.soulmatch.app/drawable/login_region_north_sherwani",
        "android.resource://com.soulmatch.app/drawable/login_region_west_gujarati"
    )

    private val generatedReligions = listOf("Hindu", "Hindu", "Hindu", "Muslim", "Christian", "Sikh", "Jain")
    private val generatedCities = generatedRegions.flatMap { it.cities }.distinct()
    private val generatedEducation = listOf("Graduate", "Post Graduate", "MBA", "Doctorate", "Professional", "M.Tech", "CA", "MBBS")
    private val generatedOccupations = listOf(
        "Software Engineer", "Product Manager", "Doctor", "Architect", "Chartered Accountant",
        "Marketing Lead", "Civil Services", "Teacher", "Founder", "Data Scientist",
        "Lawyer", "Bank Manager", "Designer", "Consultant", "Research Scientist", "HR Manager"
    )
    private val generatedIncome = listOf("5-10 LPA", "10-15 LPA", "12-18 LPA", "15-20 LPA", "18-24 LPA", "20-30 LPA", "25-35 LPA", "35+ LPA")
    private val generatedDiet = listOf("vegetarian", "jain", "eggetarian", "non_vegetarian")
    private val generatedComplexions = listOf("Fair", "Wheatish", "Dusky")
    private val generatedBodyTypes = listOf("Slim", "Average", "Athletic")
    private val generatedBloodGroups = listOf("A+", "B+", "O+", "AB+")
    private val generatedRashi = listOf("Mesh", "Vrishabh", "Mithun", "Karka", "Simha", "Kanya", "Tula", "Vrischik", "Dhanu", "Makar", "Kumbh", "Meen")
    private val generatedNakshatra = listOf("Ashwini", "Rohini", "Mrigashira", "Pushya", "Hasta", "Swati", "Anuradha", "Revati")
    private val generatedGotra = listOf("Bharadwaj", "Kashyap", "Vatsa", "Atri", "Gautam", "Harita", "Sandilya", "Vishwamitra")
    private val generatedFamilyTypes = listOf("Nuclear", "Joint")
    private val generatedMaritalStatuses = listOf("never_married", "never_married", "never_married", "divorced", "widowed")

    private val generatedProfiles = (0 until 500).map { index -> generatedProfile(index) }
    private val allProfiles = detailedProfiles + generatedProfiles

    private val curatedMatches = listOf(
        ProfileSummary(
            profileId = priyaProfileId,
            userId = priyaUserId,
            name = "Priya G.",
            age = 28,
            location = "Mumbai",
            occupation = "Doctor",
            primaryPhoto = "https://i.pravatar.cc/300?img=47",
            compatibilityScore = 96,
            compatibilityBreakdown = CompatibilityBreakdown(preferences = 97, personality = 92, horoscope = 88),
            heightCm = 162,
            isVerified = true,
            education = "Graduate",
            community = "Hindu, Vaishya",
            matchReasons = listOf("Family values align", "Vegetarian", "Similar lifestyle")
        ),
        ProfileSummary(
            profileId = nishaProfileId,
            userId = nishaUserId,
            name = "Nisha I.",
            age = 29,
            location = "Chennai",
            occupation = "Product Manager",
            primaryPhoto = "https://i.pravatar.cc/300?img=36",
            compatibilityScore = 91,
            compatibilityBreakdown = CompatibilityBreakdown(preferences = 90, personality = 93, horoscope = 84),
            heightCm = 167,
            isVerified = true,
            isPhotoPrivate = true,
            education = "Post Graduate",
            community = "Hindu, Iyer",
            matchReasons = listOf("Career fit", "City flexibility", "Profile complete")
        ),
        ProfileSummary(
            profileId = ananyaProfileId,
            userId = ananyaUserId,
            name = "Ananya D.",
            age = 27,
            location = "Ahmedabad",
            occupation = "Founder",
            primaryPhoto = "https://i.pravatar.cc/300?img=25",
            compatibilityScore = 88,
            compatibilityBreakdown = CompatibilityBreakdown(preferences = 86, personality = 89, horoscope = 82),
            heightCm = 165,
            isVerified = true,
            education = "MBA",
            community = "Hindu, Gujarati",
            matchReasons = listOf("Entrepreneurial mindset", "Strong family roots")
        ),
        ProfileSummary(
            profileId = kavyaProfileId,
            userId = kavyaUserId,
            name = "Kavya R.",
            age = 30,
            location = "Hyderabad",
            occupation = "Architect",
            primaryPhoto = "https://i.pravatar.cc/300?img=15",
            compatibilityScore = 84,
            compatibilityBreakdown = CompatibilityBreakdown(preferences = 82, personality = 85, horoscope = 80),
            heightCm = 169,
            education = "Graduate",
            community = "Hindu, Reddy",
            matchReasons = listOf("Age preference", "Creative profession", "Recent activity")
        )
    )

    val matches = curatedMatches + generatedProfiles.mapIndexed { index, profile -> generatedSummary(profile, index) }

    val savedSearches = listOf(
        SavedSearchData(searchId = "saved-1", label = "Bengaluru | 27-33 | Verified", ageMin = 27, ageMax = 33, religion = "Hindu", city = "Bengaluru"),
        SavedSearchData(searchId = "saved-2", label = "Mumbai doctors and CAs", ageMin = 25, ageMax = 34, religion = "All", city = "Mumbai"),
        SavedSearchData(searchId = "saved-3", label = "Vegetarian high compatibility", ageMin = 24, ageMax = 32, religion = "Hindu", city = "Pune")
    )

    val profilePhotos = listOf(
        ProfilePhoto(photoId = "photo-1", photoUrl = "https://i.pravatar.cc/600?img=12", isPrimary = true, sequenceOrder = 1, uploadedAt = "2026-04-20T08:00:00Z"),
        ProfilePhoto(photoId = "photo-2", photoUrl = "https://i.pravatar.cc/600?img=32", isPrimary = false, sequenceOrder = 2, uploadedAt = "2026-04-21T08:00:00Z"),
        ProfilePhoto(photoId = "photo-3", photoUrl = "https://i.pravatar.cc/600?img=45", isPrimary = false, sequenceOrder = 3, uploadedAt = "2026-04-22T08:00:00Z"),
        ProfilePhoto(photoId = "photo-4", photoUrl = "https://i.pravatar.cc/600?img=58", isPrimary = false, sequenceOrder = 4, uploadedAt = "2026-04-23T08:00:00Z")
    )

    val recentViewers = matches.drop(8).take(12).mapIndexed { index, match ->
        val detail = profileDetails(match.profileId)
        ViewerData(
            profileId = match.profileId,
            userId = match.userId,
            firstName = detail.firstName,
            lastName = detail.lastName,
            primaryPhotoUrl = match.primaryPhoto,
            viewedAt = "2026-04-${(18 + index).coerceAtMost(28).toString().padStart(2, '0')}T${(9 + index % 10).toString().padStart(2, '0')}:15:00Z"
        )
    }

    val receivedInterests = listOf(
        InterestListItem(
            interestId = "interest-1001",
            profileId = priyaProfileId,
            userId = priyaUserId,
            firstName = "Priya",
            lastName = "Gupta",
            primaryPhotoUrl = "https://i.pravatar.cc/300?img=47",
            status = "pending",
            sentAt = "2026-04-27T18:30:00Z"
        ),
        InterestListItem(
            interestId = "interest-1002",
            profileId = ananyaProfileId,
            userId = ananyaUserId,
            firstName = "Ananya",
            lastName = "Desai",
            primaryPhotoUrl = "https://i.pravatar.cc/300?img=25",
            status = "accepted",
            sentAt = "2026-04-25T12:00:00Z"
        )
    )

    val sentInterests = listOf(
        InterestListItem(
            interestId = "interest-2001",
            profileId = nishaProfileId,
            userId = nishaUserId,
            firstName = "Nisha",
            lastName = "Iyer",
            primaryPhotoUrl = "https://i.pravatar.cc/300?img=36",
            status = "pending",
            sentAt = "2026-04-26T09:00:00Z"
        )
    )

    val shortlistedProfiles = listOf(
        ShortlistItem(
            profileId = kavyaProfileId,
            userId = kavyaUserId,
            firstName = "Kavya",
            lastName = "Reddy",
            primaryPhotoUrl = "https://i.pravatar.cc/300?img=15",
            addedAt = "2026-04-24T08:00:00Z"
        )
    )

    val plans = listOf(
        PlanData("free", "SoulMatch Free", 0, "lifetime", 0, listOf("View at least 25 profiles", "Send limited interests", "View basic details")),
        PlanData("starter", "SoulMatch Starter", 499, "monthly", 30, listOf("35 interests a day", "Advanced search", "See recent viewers")),
        PlanData("plus", "SoulMatch Plus", 999, "quarterly", 90, listOf("Unlimited interests", "Priority search placement", "Chat with mutual matches")),
        PlanData("assisted", "SoulMatch Assisted", 1499, "yearly", 365, listOf("Everything in Plus", "Incognito browsing", "Dedicated relationship advisor"))
    )

    val currentSubscription = SubscriptionData(
        planId = "13",
        isActive = true,
        startDate = "2026-03-10T00:00:00Z",
        endDate = "2026-06-08T00:00:00Z"
    )

    val invoices = listOf(
        InvoiceItem(
            transactionId = "txn-3001",
            createdAt = "2026-03-10T09:30:00Z",
            amount = 5999.0,
            currency = "INR",
            status = "success",
            gateway = "razorpay",
            paymentMethod = "card",
            paymentInstrument = "Visa credit ending 1007",
            providerStatus = "captured",
            razorpayOrderId = "order_demo_3001",
            razorpayPaymentId = "pay_demo_3001",
            planId = "13",
            planName = "SoulMatch Elite",
            startDate = "2026-03-10T09:30:00Z",
            endDate = "2026-06-08T00:00:00Z",
            isActive = true
        )
    )

    val conversations = listOf(
        ConversationItem(
            chatId = makeChatId(currentUserId, ananyaUserId),
            participants = listOf(currentUserId, ananyaUserId),
            participantUserId = ananyaUserId,
            participantProfileId = ananyaProfileId,
            participantName = "Ananya Desai",
            participantPhotoUrl = "https://i.pravatar.cc/300?img=25",
            lastMessage = ChatLastMessage(
                content = "Can we plan a call this weekend?",
                type = "text",
                sentAt = "2026-04-27T19:10:00Z",
                senderId = ananyaUserId
            ),
            unreadCounts = mapOf(currentUserId to 2),
            updatedAt = "2026-04-27T19:10:00Z",
            flowId = "relationship-assist",
            flowVersionId = "relationship-assist-v1",
            flowBusinessHourType = "always",
            serviceAccountId = "soulmatch-service-account",
            operatingHoursId = "default-ist",
            flowMessages = listOf(
                BotFlowMessage(
                    flowStepId = "assist-intro-001",
                    alias = "intro_prompt",
                    content = "I can help you keep this conversation warm and family-friendly. Start with one thoughtful question about values or weekend plans.",
                    messageType = "text",
                    messageUserType = "bot",
                    messageUserAlias = "SoulMatch Assist",
                    createdMillis = 1777291200000
                ),
                BotFlowMessage(
                    flowStepId = "assist-safety-002",
                    alias = "safety_nudge",
                    content = "Share contact details only when both sides feel comfortable. You can continue using in-app chat for early introductions.",
                    messageType = "text",
                    messageUserType = "bot",
                    messageUserAlias = "SoulMatch Assist",
                    createdMillis = 1777291260000
                )
            )
        ),
        ConversationItem(
            chatId = makeChatId(currentUserId, priyaUserId),
            participants = listOf(currentUserId, priyaUserId),
            participantUserId = priyaUserId,
            participantProfileId = priyaProfileId,
            participantName = "Priya Gupta",
            participantPhotoUrl = "https://i.pravatar.cc/300?img=47",
            lastMessage = ChatLastMessage(
                content = "Thanks, I liked your profile too.",
                type = "text",
                sentAt = "2026-04-26T17:20:00Z",
                senderId = currentUserId
            ),
            unreadCounts = emptyMap(),
            updatedAt = "2026-04-26T17:20:00Z",
            flowId = "post-interest-assist",
            flowVersionId = "post-interest-assist-v1",
            flowBusinessHourType = "business_hours",
            serviceAccountId = "soulmatch-service-account",
            flowMessagesJson = """[
              {
                "flowStepId": "post-interest-001",
                "alias": "profile_context",
                "content": "Mention one detail you genuinely liked from the profile before asking for a call.",
                "messageType": "text",
                "messageUserType": "bot",
                "messageUserAlias": "SoulMatch Assist",
                "createdMillis": 1777195200000
              }
            ]"""
        )
    )

    private val messageBank = mapOf(
        ananyaUserId to listOf(
            ChatMessage(chatId = makeChatId(currentUserId, ananyaUserId), senderId = currentUserId, receiverId = ananyaUserId, content = "Hi Ananya, thanks for accepting the interest.", sentAt = "2026-04-27T18:55:00Z"),
            ChatMessage(chatId = makeChatId(currentUserId, ananyaUserId), senderId = ananyaUserId, receiverId = currentUserId, content = "Happy to connect, Rahul. Your profile felt warm and genuine.", sentAt = "2026-04-27T19:01:00Z"),
            ChatMessage(chatId = makeChatId(currentUserId, ananyaUserId), senderId = ananyaUserId, receiverId = currentUserId, content = "Can we plan a call this weekend?", sentAt = "2026-04-27T19:10:00Z")
        ),
        priyaUserId to listOf(
            ChatMessage(chatId = makeChatId(currentUserId, priyaUserId), senderId = priyaUserId, receiverId = currentUserId, content = "Hi Rahul, your family background looks lovely.", sentAt = "2026-04-26T16:55:00Z"),
            ChatMessage(chatId = makeChatId(currentUserId, priyaUserId), senderId = currentUserId, receiverId = priyaUserId, content = "Thanks, I liked your profile too.", sentAt = "2026-04-26T17:20:00Z")
        )
    )

    fun makeChatId(userOne: String, userTwo: String): String = listOf(userOne, userTwo).sorted().joinToString("_")

    fun profileDetails(profileId: String): ProfileData {
        return when (profileId) {
            currentProfileId -> myProfile
            else -> allProfiles.firstOrNull { it.profileId == profileId } ?: allProfiles.first()
        }
    }

    fun matchSeed(profileId: String): ProfileSummary? = matches.firstOrNull { it.profileId == profileId }

    fun compatibility(profileId: String): CompatibilityData {
        val summary = matchSeed(profileId)
        return CompatibilityData(
            overallScore = summary?.compatibilityScore ?: 78,
            breakdown = summary?.compatibilityBreakdown ?: CompatibilityBreakdown(preferences = 80, personality = 76, horoscope = 72)
        )
    }

    fun search(request: SearchRequest): List<ProfileSummary> {
        return matches.filter { match ->
            val withinAge = (request.ageMin == null || match.age >= request.ageMin) &&
                (request.ageMax == null || match.age <= request.ageMax)
            val matchesReligion = request.religion.isNullOrBlank() || request.religion.equals("All", true) || request.religion.equals("Any", true) ||
                match.community.contains(request.religion, ignoreCase = true)
            val matchesCity = request.city.isNullOrBlank() || match.location.contains(request.city, ignoreCase = true)
            val matchesDiet = request.diet.isNullOrBlank() || request.diet.equals("Any", true) ||
                profileDetails(match.profileId).diet.equals(request.diet, ignoreCase = true)
            withinAge && matchesReligion && matchesCity && matchesDiet
        }
    }

    fun conversationMessages(participantUserId: String): List<ChatMessage> = messageBank[participantUserId].orEmpty()

    fun conversationForParticipant(participantUserId: String): ConversationItem? {
        return conversations.firstOrNull { it.participantUserId == participantUserId || participantUserId in it.participants }
    }

    fun updateMyProfileStep(step: Int, payload: Map<String, Any>) {
        myProfile = when (step) {
            1 -> myProfile.copy(
                firstName = payload["firstName"] as? String ?: myProfile.firstName,
                lastName = payload["lastName"] as? String ?: myProfile.lastName,
                dob = payload["dob"] as? String ?: myProfile.dob,
                gender = payload["gender"] as? String ?: myProfile.gender,
                religion = payload["religion"] as? String ?: myProfile.religion,
                caste = payload["caste"] as? String ?: myProfile.caste,
                motherTongue = payload["motherTongue"] as? String ?: myProfile.motherTongue,
                maritalStatus = payload["maritalStatus"] as? String ?: myProfile.maritalStatus
            )
            2 -> myProfile.copy(
                heightCm = payload["heightCm"] as? Int ?: myProfile.heightCm,
                weightKg = payload["weightKg"] as? Int ?: myProfile.weightKg,
                complexion = payload["complexion"] as? String ?: myProfile.complexion,
                bodyType = payload["bodyType"] as? String ?: myProfile.bodyType,
                bloodGroup = payload["bloodGroup"] as? String ?: myProfile.bloodGroup
            )
            3 -> myProfile.copy(
                educationLevel = payload["educationLevel"] as? String ?: myProfile.educationLevel,
                occupation = payload["occupation"] as? String ?: myProfile.occupation,
                annualIncome = payload["annualIncome"] as? String ?: myProfile.annualIncome,
                workingCity = payload["workingCity"] as? String ?: myProfile.workingCity
            )
            4 -> myProfile.copy(
                fatherOccupation = payload["fatherOccupation"] as? String ?: myProfile.fatherOccupation,
                motherOccupation = payload["motherOccupation"] as? String ?: myProfile.motherOccupation,
                numBrothers = payload["numBrothers"] as? Int ?: myProfile.numBrothers,
                numSisters = payload["numSisters"] as? Int ?: myProfile.numSisters,
                familyType = payload["familyType"] as? String ?: myProfile.familyType,
                familyCity = payload["familyCity"] as? String ?: myProfile.familyCity
            )
            5 -> myProfile.copy(
                diet = payload["diet"] as? String ?: myProfile.diet,
                smoking = payload["smoking"] as? String ?: myProfile.smoking,
                drinking = payload["drinking"] as? String ?: myProfile.drinking,
                aboutMe = payload["aboutMe"] as? String ?: myProfile.aboutMe
            )
            6 -> myProfile.copy(
                rashi = payload["rashi"] as? String ?: myProfile.rashi,
                nakshatra = payload["nakshatra"] as? String ?: myProfile.nakshatra,
                isManglik = payload["isManglik"] as? Boolean ?: myProfile.isManglik,
                birthCity = payload["birthCity"] as? String ?: myProfile.birthCity,
                gotra = payload["gotra"] as? String ?: myProfile.gotra
            )
            else -> myProfile
        }.copy(completionScore = 96)
    }

    private fun generatedProfile(index: Int): ProfileData {
        val region = generatedRegions[index % generatedRegions.size]
        val gender = if (index % 2 == 0) "female" else "male"
        val firstName = if (gender == "male") {
            region.maleNames[(index / generatedRegions.size) % region.maleNames.size]
        } else {
            region.femaleNames[(index / generatedRegions.size) % region.femaleNames.size]
        }
        val lastName = region.lastNames[(index * 3) % region.lastNames.size]
        val city = region.cities[(index * 5) % region.cities.size]
        val religion = when {
            region.motherTongue == "Punjabi" && index % 3 == 0 -> "Sikh"
            region.motherTongue == "Malayalam" && index % 5 == 0 -> "Christian"
            region.motherTongue == "Gujarati" && index % 6 == 0 -> "Jain"
            else -> generatedReligions[index % generatedReligions.size].takeUnless { it == "Sikh" || it == "Jain" } ?: "Hindu"
        }
        val caste = region.communities[(index * 2) % region.communities.size]
        val age = 21 + (index % 20)
        val year = 2026 - age
        val month = 1 + (index % 12)
        val day = 1 + ((index * 7) % 27)
        return ProfileData(
            profileId = "dummy-profile-${(index + 1).toString().padStart(4, '0')}",
            userId = "dummy-user-${(index + 1).toString().padStart(4, '0')}",
            firstName = firstName,
            lastName = lastName,
            dob = "$year-${month.toString().padStart(2, '0')}-${day.toString().padStart(2, '0')}",
            age = age,
            gender = gender,
            religion = religion,
            caste = caste,
            motherTongue = region.motherTongue,
            maritalStatus = generatedMaritalStatuses[index % generatedMaritalStatuses.size],
            completionScore = 72 + (index % 28),
            profileCreatedBy = if (index % 7 == 0) "mediator" else "self",
            primaryPhotoUrl = generatedProfileImages[index % generatedProfileImages.size],
            photoPrivacy = if (index % 9 == 0) "matches_only" else "all",
            educationLevel = generatedEducation[(index * 3) % generatedEducation.size],
            occupation = generatedOccupations[(index * 5) % generatedOccupations.size],
            annualIncome = generatedIncome[(index * 2) % generatedIncome.size],
            workingCity = city,
            heightCm = 154 + (index % 32),
            weightKg = 48 + (index % 35),
            complexion = generatedComplexions[index % generatedComplexions.size],
            bodyType = generatedBodyTypes[(index + 1) % generatedBodyTypes.size],
            bloodGroup = generatedBloodGroups[index % generatedBloodGroups.size],
            fatherOccupation = listOf("Business", "Retired banker", "Professor", "Government service", "Doctor", "Civil contractor")[index % 6],
            motherOccupation = listOf("Homemaker", "Teacher", "Doctor", "Retired officer", "Entrepreneur", "Professor")[(index + 2) % 6],
            numBrothers = index % 3,
            numSisters = (index + 1) % 3,
            familyType = generatedFamilyTypes[index % generatedFamilyTypes.size],
            familyCity = region.cities[(index * 7 + 3) % region.cities.size],
            diet = generatedDiet[index % generatedDiet.size],
            smoking = if (index % 11 == 0) "occasionally" else "never",
            drinking = when (index % 4) {
                0 -> "never"
                1 -> "socially"
                else -> "occasionally"
            },
            aboutMe = "$firstName comes from a ${region.motherTongue}-speaking family, values respectful introductions, career growth, and a partnership where both families feel included.",
            rashi = generatedRashi[index % generatedRashi.size],
            nakshatra = generatedNakshatra[index % generatedNakshatra.size],
            isManglik = index % 8 == 0,
            birthCity = region.cities[(index * 6 + 1) % region.cities.size],
            gotra = generatedGotra[index % generatedGotra.size]
        )
    }

    private fun generatedSummary(profile: ProfileData, index: Int): ProfileSummary {
        val score = 62 + ((index * 7) % 38)
        val reasons = listOf(
            listOf("Family values align", "Parents involved", "Similar city preference"),
            listOf("Career fit", "Education match", "Recently active"),
            listOf("Lifestyle fit", "Diet preference", "Profile complete"),
            listOf("Tradition context", "Language match", "High response intent")
        )[index % 4]
        return ProfileSummary(
            profileId = profile.profileId,
            userId = profile.userId,
            name = "${profile.firstName} ${profile.lastName.take(1)}.",
            age = profile.age,
            location = profile.workingCity,
            occupation = profile.occupation,
            primaryPhoto = profile.primaryPhotoUrl,
            compatibilityScore = score,
            compatibilityBreakdown = CompatibilityBreakdown(
                preferences = (score + 4).coerceAtMost(99),
                personality = (score - 2).coerceAtLeast(55),
                horoscope = (score - 7).coerceAtLeast(50)
            ),
            heightCm = profile.heightCm,
            isVerified = index % 3 != 0,
            isPhotoPrivate = profile.photoPrivacy == "matches_only",
            education = profile.educationLevel,
            community = "${profile.religion}, ${profile.caste}",
            lastActiveLabel = when (index % 5) {
                0 -> "Active today"
                1 -> "Active this week"
                2 -> "Viewed your profile"
                3 -> "New this week"
                else -> "Recently active"
            },
            matchReasons = reasons,
            interestSent = index % 13 == 0,
            shortlisted = index % 17 == 0,
            profileCreatedBy = profile.profileCreatedBy
        )
    }
}
