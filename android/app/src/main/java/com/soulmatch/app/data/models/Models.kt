package com.soulmatch.app.data.models

import com.google.gson.annotations.SerializedName

data class GenericResponse<T>(
    val success: Boolean,
    val data: T? = null,
    val error: ErrorData? = null,
    val message: String? = null
)

data class ErrorData(
    val code: String = "",
    val message: String = ""
)

data class SendOTPRequest(val phone: String, val userType: String? = null)
data class VerifyOTPRequest(
    val phone: String,
    val otp: String,
    val userType: String? = null
)
data class GoogleLoginRequest(
    val googleToken: String,
    val userType: String? = null
)
data class SelectUserTypeRequest(
    val userType: String
)
data class FirebasePhoneLoginRequest(
    val firebaseToken: String,
    val phone: String? = null,
    val inviteCode: String? = null,
    val acquisitionSource: String? = null,
    val userType: String? = null
)
data class RefreshTokenRequest(val refreshToken: String)
data class FcmTokenRequest(val token: String)
data class AnalyticsEventRequest(
    val eventType: String,
    val serviceName: String = "android-app",
    val userId: String? = null,
    val sessionId: String? = null,
    val page: String? = null,
    val target: String? = null,
    val appVersion: String? = null,
    val payload: Map<String, String> = emptyMap()
)
data class AnalyticsBatchRequest(
    val events: List<AnalyticsEventRequest>
)

data class AuthData(
    val accessToken: String,
    val refreshToken: String,
    val userId: String,
    val isNewUser: Boolean,
    val userType: String = "member",
    val requiresRoleSelection: Boolean = false
)

data class AgentServiceAreaData(
    @SerializedName("advisorServiceAreaId") val advisorServiceAreaId: String = "",
    val city: String = "",
    val state: String = "",
    val locality: String = "",
    val pincode: String = "",
    @SerializedName("radiusKm") val radiusKm: Int = 15,
    @SerializedName("isPrimary") val isPrimary: Boolean = false
)

data class AgentProfileData(
    @SerializedName("advisorId") val advisorId: String? = null,
    @SerializedName("userId") val userId: String = "",
    @SerializedName("userType") val userType: String = "agent",
    @SerializedName("agentCode") val agentCode: String = "",
    @SerializedName("fullName") val fullName: String = "",
    val phone: String = "",
    val email: String = "",
    @SerializedName("businessName") val businessName: String = "",
    @SerializedName("referralCode") val referralCode: String = "",
    @SerializedName("serviceLabel") val serviceLabel: String = "SoulMatch Advisor",
    val bio: String = "",
    val city: String = "",
    val state: String = "",
    val pincode: String = "",
    @SerializedName("profilePhotoUrl") val profilePhotoUrl: String = "",
    @SerializedName("yearsExperience") val yearsExperience: Int = 0,
    val languages: List<String> = emptyList(),
    val communities: List<String> = emptyList(),
    @SerializedName("membershipPlan") val membershipPlan: String = "free",
    @SerializedName("membershipExpiresAt") val membershipExpiresAt: String? = null,
    @SerializedName("autoRenew") val autoRenew: Boolean = false,
    @SerializedName("contactViewsUsed") val contactViewsUsed: Int = 0,
    @SerializedName("kycStatus") val kycStatus: String = "pending",
    @SerializedName("onboardingStatus") val onboardingStatus: String = "pending",
    @SerializedName("onboardingRejectionReason") val onboardingRejectionReason: String = "",
    @SerializedName("aadhaarVerificationStatus") val aadhaarVerificationStatus: String = "not_started",
    @SerializedName("panVerificationStatus") val panVerificationStatus: String = "not_started",
    @SerializedName("kycNameMatchStatus") val kycNameMatchStatus: String = "pending",
    @SerializedName("bankVerificationStatus") val bankVerificationStatus: String = "not_started",
    @SerializedName("bankName") val bankName: String = "",
    @SerializedName("bankAccountLast4") val bankAccountLast4: String = "",
    @SerializedName("bankIfsc") val bankIfsc: String = "",
    @SerializedName("bankNameMatchStatus") val bankNameMatchStatus: String = "pending",
    @SerializedName("pennyDropStatus") val pennyDropStatus: String = "not_started",
    @SerializedName("pennyDropOrderId") val pennyDropOrderId: String = "",
    @SerializedName("pennyDropAmountPaise") val pennyDropAmountPaise: Int = 100,
    @SerializedName("pennyDropNameMatchStatus") val pennyDropNameMatchStatus: String = "pending",
    @SerializedName("termsAcceptedAt") val termsAcceptedAt: String? = null,
    @SerializedName("termsVersion") val termsVersion: String = "",
    @SerializedName("fraudReviewStatus") val fraudReviewStatus: String = "pending",
    val status: String = "active",
    @SerializedName("feePreferences") val feePreferences: Map<String, String> = emptyMap(),
    @SerializedName("successRate") val successRate: Double = 0.0,
    @SerializedName("averageRating") val averageRating: Double = 0.0,
    @SerializedName("complaintScore") val complaintScore: Double = 0.0,
    @SerializedName("serviceAreas") val serviceAreas: List<AgentServiceAreaData> = emptyList(),
    @SerializedName("kycDocuments") val kycDocuments: List<AgentKycDocumentData> = emptyList(),
    @SerializedName("isOnboarded") val isOnboarded: Boolean = false
)

data class AgentKycDocumentData(
    @SerializedName("advisorKycDocumentId") val advisorKycDocumentId: String = "",
    @SerializedName("documentType") val documentType: String = "",
    @SerializedName("documentSide") val documentSide: String = "single",
    @SerializedName("fileUrl") val fileUrl: String = "",
    val status: String = "uploaded",
    @SerializedName("reviewComment") val reviewComment: String = "",
    @SerializedName("isEncrypted") val isEncrypted: Boolean = false,
    @SerializedName("contentSha256") val contentSha256: String = "",
    @SerializedName("originalFileName") val originalFileName: String = "",
    @SerializedName("mimeType") val mimeType: String = ""
)

data class AgentProfileUpsertRequest(
    val fullName: String,
    val email: String = "",
    val phone: String = "",
    val city: String,
    val state: String = "",
    val businessName: String = "",
    val referralCode: String = "",
    val locality: String = "",
    val pincode: String = "",
    val languages: List<String> = emptyList(),
    val communities: List<String> = emptyList(),
    val yearsExperience: Int = 0,
    val bio: String = "",
    val serviceLabel: String = "SoulMatch Advisor",
    val status: String = "active",
    val termsAccepted: Boolean = false,
    val termsVersion: String = "agent-terms-2026-05-10-v1",
    val feePreferences: Map<String, String> = emptyMap()
)

data class AgentOnboardingRequest(
    val fullName: String,
    val phone: String,
    val email: String = "",
    val city: String,
    val state: String,
    val businessName: String,
    val referralCode: String = "",
    val serviceLabel: String = "SoulMatch Agent",
    val yearsExperience: Int = 0,
    val languages: List<String> = emptyList(),
    val termsAccepted: Boolean = false,
    val termsVersion: String = "agent-terms-2026-05-10-v1",
    val serviceAreas: List<AgentServiceAreaData> = emptyList(),
    val kycDocuments: List<AgentKycDocumentInput> = emptyList()
)

data class AgentKycDocumentInput(
    val documentType: String,
    val documentSide: String = "single",
    val fileUrl: String
)

data class AgentManagedProfileSummaryData(
    @SerializedName("profileId") val profileId: String = "",
    @SerializedName("userId") val userId: String = "",
    @SerializedName("firstName") val firstName: String = "",
    @SerializedName("lastName") val lastName: String = "",
    val gender: String = "",
    val dob: String? = null,
    val religion: String = "",
    val caste: String = "",
    @SerializedName("motherTongue") val motherTongue: String = "",
    @SerializedName("primaryPhotoUrl") val primaryPhotoUrl: String = "",
    @SerializedName("completionScore") val completionScore: Int = 0,
    @SerializedName("reviewStatus") val reviewStatus: String = "draft",
    @SerializedName("verificationStatus") val verificationStatus: String = "pending",
    @SerializedName("rejectionReason") val rejectionReason: String = "",
    val occupation: String = "",
    @SerializedName("annualIncome") val annualIncome: String = "",
    val city: String = "",
    val state: String = "",
    @SerializedName("viewCount") val viewCount: Int = 0,
    @SerializedName("matchCount") val matchCount: Int = 0,
    @SerializedName("documentChecklistPercent") val documentChecklistPercent: Int = 0,
    @SerializedName("profileSource") val profileSource: String = "managed",
    @SerializedName("createdAt") val createdAt: String? = null,
    @SerializedName("updatedAt") val updatedAt: String? = null
)

data class AgentManagedProfileCreateRequest(
    val firstName: String,
    val lastName: String = "",
    val dob: String,
    val gender: String,
    val religion: String,
    val caste: String = "",
    val motherTongue: String = "",
    val maritalStatus: String = "never_married",
    val heightCm: Int? = null,
    val weightKg: Int? = null,
    val complexion: String = "",
    val educationLevel: String = "",
    val occupation: String = "",
    val annualIncome: String = "",
    val city: String = "",
    val state: String = "",
    val pincode: String = "",
    val mobile: String = "",
    val email: String = "",
    val fatherOccupation: String = "",
    val motherOccupation: String = "",
    val numBrothers: Int = 0,
    val numSisters: Int = 0,
    val familyType: String = "",
    val diet: String = "",
    val aboutMe: String = ""
)

data class AgentMembershipData(
    @SerializedName("planId") val planId: String = "free",
    @SerializedName("monthlyPrice") val monthlyPrice: Int = 0,
    @SerializedName("profilesAllowed") val profilesAllowed: Int = 5,
    @SerializedName("visibleMatches") val visibleMatches: Int = 10,
    @SerializedName("contactViews") val contactViews: Int = 0,
    @SerializedName("hasAnalytics") val hasAnalytics: Boolean = false,
    @SerializedName("hasRelationshipManager") val hasRelationshipManager: Boolean = false,
    @SerializedName("featuredBadge") val featuredBadge: Boolean = false,
    @SerializedName("autoRenew") val autoRenew: Boolean = false,
    @SerializedName("contactViewsUsed") val contactViewsUsed: Int = 0,
    @SerializedName("membershipExpiresAt") val membershipExpiresAt: String? = null,
    @SerializedName("onboardingStatus") val onboardingStatus: String = "pending"
)

data class AgentLeadData(
    @SerializedName("profileId") val profileId: String = "",
    @SerializedName("userId") val userId: String = "",
    val name: String = "",
    val age: Int = 0,
    val religion: String = "",
    val community: String = "",
    @SerializedName("motherTongue") val motherTongue: String = "",
    @SerializedName("workingCity") val workingCity: String = "",
    @SerializedName("familyCity") val familyCity: String = "",
    val occupation: String = "",
    @SerializedName("primaryPhotoUrl") val primaryPhotoUrl: String? = null,
    @SerializedName("trustScore") val trustScore: Int = 0,
    @SerializedName("trustLevel") val trustLevel: String = "low",
    @SerializedName("supportLevel") val supportLevel: String = "advisor_assisted",
    @SerializedName("requestStatus") val requestStatus: String = "assigned",
    @SerializedName("familyContactName") val familyContactName: String = "",
    @SerializedName("familyContactPhone") val familyContactPhone: String = "",
    @SerializedName("preferredContactWindow") val preferredContactWindow: String = "",
    val notes: String = "",
    @SerializedName("assignedAt") val assignedAt: String? = null,
    @SerializedName("nextReviewAt") val nextReviewAt: String? = null
)

data class ProfileStepResponse(
    val profileId: String = "",
    val completionScore: Int = 0,
    val step: Int = 1
)

data class AiBioSuggestionRequest(
    val currentBio: String = ""
)

data class AiSuggestionData(
    val source: String = "rules",
    val provider: String = "local",
    val model: String = "fallback",
    val suggestions: List<String> = emptyList(),
    val notice: String = ""
)

data class IcebreakerRequest(
    val intent: String = "first_message"
)

data class IcebreakerSuggestionData(
    val source: String = "rules",
    val provider: String = "local",
    val model: String = "fallback",
    val suggestions: List<String> = emptyList(),
    val safetyNote: String = ""
)

data class PartnerPreferencesData(
    @SerializedName("age_min") val ageMin: Int = 24,
    @SerializedName("age_max") val ageMax: Int = 32,
    val religion: String? = null,
    @SerializedName("manglik_pref") val manglikPref: String = "any",
    @SerializedName("education_levels") val educationLevels: List<String> = emptyList(),
    val occupations: List<String> = emptyList(),
    @SerializedName("annual_income_min") val annualIncomeMin: Int? = null,
    @SerializedName("annual_income_max") val annualIncomeMax: Int? = null,
    @SerializedName("height_min_cm") val heightMinCm: Int? = null,
    @SerializedName("height_max_cm") val heightMaxCm: Int? = null,
    val locations: List<String> = emptyList(),
    @SerializedName("location_radius_km") val locationRadiusKm: Int = 50,
    @SerializedName("location_preferences") val locationPreferences: List<String> = emptyList(),
    @SerializedName("diet_prefs") val dietPrefs: List<String> = emptyList(),
    @SerializedName("income_preferences") val incomePreferences: List<String> = emptyList(),
    @SerializedName("lifestyle_preferences") val lifestylePreferences: List<String> = emptyList(),
    @SerializedName("marital_statuses") val maritalStatuses: List<String> = emptyList(),
    @SerializedName("family_types") val familyTypes: List<String> = emptyList(),
    @SerializedName("relocation_open") val relocationOpen: Boolean? = null,
    val timeline: String? = null,
    @SerializedName("deal_breakers") val dealBreakers: List<String> = emptyList(),
    @SerializedName("good_to_have") val goodToHave: List<String> = emptyList()
)

data class MatchFeedbackRequest(
    val action: String = "viewed",
    val reason: String? = null,
    val note: String? = null,
    val metadata: Map<String, String> = emptyMap()
)

data class PrivacySettingsRequest(
    @SerializedName("photoPrivacy") val photoPrivacy: String,
    @SerializedName("profileVisibility") val profileVisibility: String,
    @SerializedName("hideLastSeen") val hideLastSeen: Boolean = false,
    @SerializedName("contactPrivacy") val contactPrivacy: String? = null
)

data class ContactUnlockData(
    val status: String = "",
    val canUnmask: Boolean = false,
    val phone: String = "",
    val email: String = "",
    val maskedPhone: String = "",
    val maskedEmail: String = "",
    val remaining: Int? = null,
    val message: String = ""
)

data class ProfileStatusRequest(
    @SerializedName("profileStatus") val profileStatus: String
)

data class PhotoAccessRequestBody(
    val message: String = ""
)

data class PhotoAccessActionRequest(
    val status: String
)

data class PhotoAccessResponseData(
    val requestId: String = "",
    val status: String = ""
)

data class VerificationSubmitRequest(
    val type: String = "profile",
    @SerializedName("documentUrl") val documentUrl: String? = null
)

data class VerificationRequestData(
    @SerializedName("verification_id") val verificationId: String = "",
    @SerializedName("user_id") val userId: String = "",
    val type: String = "profile",
    val status: String = "pending",
    @SerializedName("document_url") val documentUrl: String? = null,
    @SerializedName("reviewer_email") val reviewerEmail: String? = null,
    @SerializedName("review_note") val reviewNote: String? = null,
    @SerializedName("reviewed_at") val reviewedAt: String? = null,
    @SerializedName("created_at") val createdAt: String = ""
)

data class ViewerData(
    @SerializedName("profile_id") val profileId: String = "",
    @SerializedName("user_id") val userId: String = "",
    @SerializedName("first_name") val firstName: String = "",
    @SerializedName("last_name") val lastName: String = "",
    @SerializedName("primary_photo_url") val primaryPhotoUrl: String? = null,
    @SerializedName("viewed_at") val viewedAt: String = ""
)

data class PhotoAccessRequestData(
    @SerializedName("photo_access_request_id") val requestId: String = "",
    @SerializedName("target_profile_id") val targetProfileId: String = "",
    @SerializedName("requester_profile_id") val requesterProfileId: String = "",
    @SerializedName("requester_user_id") val requesterUserId: String = "",
    val status: String = "pending",
    val message: String? = null,
    @SerializedName("requested_at") val requestedAt: String = "",
    @SerializedName("responded_at") val respondedAt: String? = null,
    @SerializedName("first_name") val firstName: String = "",
    @SerializedName("last_name") val lastName: String = "",
    @SerializedName("primary_photo_url") val primaryPhotoUrl: String? = null,
    val occupation: String = "",
    @SerializedName("working_city") val workingCity: String = ""
)

data class AssistStatusRequest(
    @SerializedName("isOptedIn") val isOptedIn: Boolean,
    @SerializedName("supportLevel") val supportLevel: String,
    @SerializedName("shareMode") val shareMode: String = "single",
    @SerializedName("selectedAdvisorIds") val selectedAdvisorIds: List<String> = emptyList(),
    @SerializedName("preferredContactWindow") val preferredContactWindow: String = "",
    @SerializedName("familyContactName") val familyContactName: String = "",
    @SerializedName("familyContactPhone") val familyContactPhone: String = "",
    val notes: String = ""
)

data class AssistLocationData(
    val city: String = "",
    val state: String = "",
    val locality: String = "",
    val pincode: String = ""
)

data class AdvisorSummaryData(
    @SerializedName("advisorId") val advisorId: String = "",
    @SerializedName("fullName") val fullName: String = "",
    val phone: String = "",
    val email: String = "",
    @SerializedName("serviceLabel") val serviceLabel: String = "SoulMatch Advisor",
    val bio: String = "",
    val city: String = "",
    val state: String = "",
    val pincode: String = "",
    val locality: String = "",
    val languages: List<String> = emptyList(),
    val communities: List<String> = emptyList(),
    @SerializedName("averageRating") val averageRating: Double = 0.0,
    @SerializedName("successRate") val successRate: Double = 0.0,
    @SerializedName("activeAssignments") val activeAssignments: Int = 0,
    val score: Double = 0.0,
    val reasons: List<String> = emptyList(),
    @SerializedName("isSelected") val isSelected: Boolean = false
)

data class AssistReadinessData(
    @SerializedName("hasCity") val hasCity: Boolean = false,
    @SerializedName("hasPincode") val hasPincode: Boolean = false,
    @SerializedName("canAutoAssign") val canAutoAssign: Boolean = false
)

data class AssistAgentStatsData(
    @SerializedName("activeCount") val activeCount: Int = 0,
    @SerializedName("verifiedCount") val verifiedCount: Int = 0,
    @SerializedName("unverifiedCount") val unverifiedCount: Int = 0
)

data class AssistStatusData(
    @SerializedName("profileId") val profileId: String = "",
    @SerializedName("isOptedIn") val isOptedIn: Boolean = false,
    @SerializedName("supportLevel") val supportLevel: String = "self_service",
    @SerializedName("shareMode") val shareMode: String = "single",
    @SerializedName("selectedAdvisorIds") val selectedAdvisorIds: List<String> = emptyList(),
    @SerializedName("requestStatus") val requestStatus: String = "not_requested",
    @SerializedName("preferredContactWindow") val preferredContactWindow: String = "",
    @SerializedName("familyContactName") val familyContactName: String = "",
    @SerializedName("familyContactPhone") val familyContactPhone: String = "",
    val notes: String = "",
    @SerializedName("assignedAt") val assignedAt: String? = null,
    @SerializedName("nextReviewAt") val nextReviewAt: String? = null,
    val location: AssistLocationData = AssistLocationData(),
    val readiness: AssistReadinessData = AssistReadinessData(),
    @SerializedName("agentStats") val agentStats: AssistAgentStatsData = AssistAgentStatsData(),
    val advisor: AdvisorSummaryData? = null,
    val recommendations: List<AdvisorSummaryData> = emptyList(),
    val agents: List<AdvisorSummaryData> = emptyList()
)

data class ProfilePhoto(
    @SerializedName("photo_id") val photoId: String = "",
    @SerializedName("photo_url") val photoUrl: String = "",
    @SerializedName("is_primary") val isPrimary: Boolean = false,
    @SerializedName("sequence_order") val sequenceOrder: Int? = null,
    @SerializedName("uploaded_at") val uploadedAt: String = ""
)

data class PhotoUploadData(
    @SerializedName("photoUrls") val photoUrls: List<String> = emptyList()
)

data class TrustFactorData(
    val key: String = "",
    val label: String = "",
    val points: Int = 0,
    val status: String = "missing",
    val detail: String = ""
)

data class TrustExplanationData(
    val summary: String = "",
    val approvedVerificationTypes: List<String> = emptyList()
)

data class ProfileData(
    @SerializedName("profile_id") val profileId: String = "",
    @SerializedName("user_id") val userId: String = "",
    @SerializedName("first_name") val firstName: String = "",
    @SerializedName("last_name") val lastName: String = "",
    val dob: String? = null,
    val age: Int = 0,
    val gender: String = "",
    val phone: String = "",
    val email: String = "",
    @SerializedName("is_phone_verified") val isPhoneVerified: Boolean = false,
    val religion: String = "",
    val caste: String = "",
    @SerializedName("mother_tongue") val motherTongue: String = "",
    @SerializedName("marital_status") val maritalStatus: String = "",
    @SerializedName("completion_score") val completionScore: Int = 0,
    @SerializedName("is_partner_pref_set") val isPartnerPrefSet: Boolean = false,
    @SerializedName("profile_status") val profileStatus: String = "active",
    @SerializedName("profile_created_by") val profileCreatedBy: String = "self",
    @SerializedName("verification_status") val verificationStatus: String = "pending",
    @SerializedName("trust_score") val trustScore: Int = 0,
    @SerializedName("trust_level") val trustLevel: String = "low",
    @SerializedName("trust_signals") val trustSignals: List<String> = emptyList(),
    @SerializedName("trust_warnings") val trustWarnings: List<String> = emptyList(),
    @SerializedName("trust_factors") val trustFactors: List<TrustFactorData> = emptyList(),
    @SerializedName("trust_explanation") val trustExplanation: TrustExplanationData? = null,
    @SerializedName("seriousness_score") val seriousnessScore: Int = 0,
    @SerializedName("seriousness_level") val seriousnessLevel: String = "low",
    @SerializedName("seriousness_signals") val seriousnessSignals: List<String> = emptyList(),
    @SerializedName("seriousness_warnings") val seriousnessWarnings: List<String> = emptyList(),
    @SerializedName("primary_photo_url") val primaryPhotoUrl: String? = null,
    @SerializedName("photo_privacy") val photoPrivacy: String = "all",
    @SerializedName("contact_privacy") val contactPrivacy: String = "visible",
    @SerializedName("contact_access_status") val contactAccessStatus: String = "masked",
    @SerializedName("can_unmask_contact") val canUnmaskContact: Boolean = false,
    @SerializedName("contact_unlocks_remaining") val contactUnlocksRemaining: Int? = null,
    @SerializedName("contact_access_message") val contactAccessMessage: String = "",
    @SerializedName("masked_phone") val maskedPhone: String = "",
    @SerializedName("masked_email") val maskedEmail: String = "",
    @SerializedName("can_view_photo") val canViewPhoto: Boolean = true,
    @SerializedName("photo_access_status") val photoAccessStatus: String = "visible",
    @SerializedName("photo_access_request_id") val photoAccessRequestId: String? = null,
    @SerializedName("profile_visibility") val profileVisibility: String = "all",
    @SerializedName("hide_last_seen") val hideLastSeen: Boolean = false,
    @SerializedName("last_login") val lastLogin: String? = null,
    @SerializedName("updated_at") val updatedAt: String? = null,
    @SerializedName("education_level") val educationLevel: String = "",
    @SerializedName("no_education") val noEducation: Boolean = false,
    @SerializedName("is_employed") val isEmployed: Boolean = false,
    val occupation: String = "",
    @SerializedName("annual_income") val annualIncome: String = "",
    @SerializedName("working_city") val workingCity: String = "",
    @SerializedName("working_state") val workingState: String = "",
    @SerializedName("working_pincode") val workingPincode: String = "",
    @SerializedName("native_place") val nativePlace: String = "",
    @SerializedName("height_cm") val heightCm: Int? = null,
    @SerializedName("weight_kg") val weightKg: Int? = null,
    val complexion: String = "",
    @SerializedName("body_type") val bodyType: String = "",
    @SerializedName("blood_group") val bloodGroup: String = "",
    @SerializedName("father_occupation") val fatherOccupation: String = "",
    @SerializedName("mother_occupation") val motherOccupation: String = "",
    @SerializedName("num_brothers") val numBrothers: Int? = null,
    @SerializedName("num_sisters") val numSisters: Int? = null,
    @SerializedName("family_type") val familyType: String = "",
    @SerializedName("family_city") val familyCity: String = "",
    @SerializedName("family_state") val familyState: String = "",
    @SerializedName("family_locality") val familyLocality: String = "",
    @SerializedName("family_pincode") val familyPincode: String = "",
    @SerializedName("family_status") val familyStatus: String = "",
    @SerializedName("about_family") val aboutFamily: String = "",
    val diet: String = "",
    val smoking: String = "",
    val drinking: String = "",
    @SerializedName("about_me") val aboutMe: String = "",
    @SerializedName("sub_caste") val subCaste: String = "",
    @SerializedName("religious_values") val religiousValues: String = "",
    @SerializedName("institution_name") val institutionName: String = "",
    @SerializedName("company_name") val companyName: String = "",
    @SerializedName("work_location") val workLocation: String = "",
    val hobbies: List<String> = emptyList(),
    @SerializedName("languages_known") val languagesKnown: List<String> = emptyList(),
    @SerializedName("personality_traits") val personalityTraits: List<String> = emptyList(),
    val rashi: String = "",
    val nakshatra: String = "",
    @SerializedName("is_manglik") val isManglik: Boolean = false,
    @SerializedName("birth_city") val birthCity: String = "",
    val gotra: String = "",
    @SerializedName("location_preferences") val locationPreferences: List<String> = emptyList(),
    @SerializedName("income_preferences") val incomePreferences: List<String> = emptyList(),
    @SerializedName("lifestyle_preferences") val lifestylePreferences: List<String> = emptyList(),
    @SerializedName("partner_preferences") val partnerPreferences: PartnerPreferencesData? = null
)

data class CompatibilityBreakdown(
    val preferences: Int = 0,
    val personality: Int = 0,
    val horoscope: Int = 0
)

data class ProfileSummary(
    @SerializedName("profileId") val profileId: String = "",
    @SerializedName("userId") val userId: String = "",
    val name: String = "",
    val age: Int = 0,
    val gender: String = "",
    val location: String = "",
    val occupation: String = "",
    @SerializedName("primaryPhoto") val primaryPhoto: String? = null,
    @SerializedName("compatibilityScore") val compatibilityScore: Int = 0,
    @SerializedName("compatibilityBreakdown") val compatibilityBreakdown: CompatibilityBreakdown? = null,
    @SerializedName("heightCm") val heightCm: Int? = null,
    @SerializedName("isVerified") val isVerified: Boolean = false,
    @SerializedName("isPhotoPrivate") val isPhotoPrivate: Boolean = false,
    @SerializedName("trustScore") val trustScore: Int = 0,
    @SerializedName("trustLevel") val trustLevel: String = "low",
    @SerializedName("trustSignals") val trustSignals: List<String> = emptyList(),
    @SerializedName("trustFactors") val trustFactors: List<TrustFactorData> = emptyList(),
    val education: String = "",
    val community: String = "",
    val religion: String = "",
    val annualIncome: String = "",
    val familyCity: String = "",
    val familyState: String = "",
    val maritalStatus: String = "",
    val diet: String = "",
    @SerializedName("isManglik") val isManglik: Boolean = false,
    @SerializedName("createdAt") val createdAt: String = "",
    @SerializedName("hideLastSeen") val hideLastSeen: Boolean = false,
    val lastActiveLabel: String = "Recently Active",
    val matchReasons: List<String> = emptyList(),
    val interestSent: Boolean = false,
    val shortlisted: Boolean = false,
    @SerializedName("profileCreatedBy") val profileCreatedBy: String = "self"
)

data class MatchesData(
    val matches: List<ProfileSummary> = emptyList(),
    val total: Int = 0,
    val page: Int = 1,
    val limit: Int = 25
)

data class CompatibilityData(
    val overallScore: Int = 0,
    val breakdown: CompatibilityBreakdown? = null
)

data class SearchRequest(
    val ageMin: Int? = null,
    val ageMax: Int? = null,
    val religion: String? = null,
    @SerializedName("location") val city: String? = null,
    val gender: String? = null,
    val diet: String? = null,
    val community: String? = null,
    val motherTongue: String? = null,
    val education: String? = null,
    val occupation: String? = null,
    val income: String? = null,
    val familyType: String? = null,
    val maritalStatus: String? = null,
    val manglik: String? = null,
    val verifiedOnly: Boolean = false,
    val photoOnly: Boolean = false,
    val recentlyActiveOnly: Boolean = false,
    val page: Int = 1,
    val limit: Int = 25
)

data class SearchProfileItem(
    @SerializedName("profile_id") val profileId: String = "",
    @SerializedName("user_id") val userId: String = "",
    @SerializedName("first_name") val firstName: String = "",
    @SerializedName("last_name") val lastName: String = "",
    val age: Int = 0,
    val gender: String = "",
    val phone: String = "",
    val email: String = "",
    @SerializedName("is_phone_verified") val isPhoneVerified: Boolean = false,
    val religion: String = "",
    val caste: String = "",
    @SerializedName("primary_photo_url") val primaryPhotoUrl: String? = null,
    val occupation: String = "",
    @SerializedName("working_city") val workingCity: String = "",
    @SerializedName("education_level") val educationLevel: String = "",
    @SerializedName("annual_income") val annualIncome: String = "",
    @SerializedName("height_cm") val heightCm: Int? = null,
    val diet: String = "",
    @SerializedName("family_city") val familyCity: String = "",
    @SerializedName("family_state") val familyState: String = "",
    @SerializedName("marital_status") val maritalStatus: String = "",
    @SerializedName("is_manglik") val isManglik: Boolean = false,
    @SerializedName("created_at") val createdAt: String = "",
    @SerializedName("hide_last_seen") val hideLastSeen: Boolean = false,
    @SerializedName("profile_created_by") val profileCreatedBy: String = "self",
    @SerializedName("is_photo_private") val isPhotoPrivate: Boolean = false,
    @SerializedName("is_verified") val isVerified: Boolean = false,
    @SerializedName("trust_score") val trustScore: Int = 0,
    @SerializedName("trust_level") val trustLevel: String = "low",
    @SerializedName("trust_signals") val trustSignals: List<String> = emptyList(),
    @SerializedName("trust_factors") val trustFactors: List<TrustFactorData> = emptyList(),
    @SerializedName("match_reasons") val matchReasons: List<String> = emptyList(),
    @SerializedName("last_active_label") val lastActiveLabelSnake: String = ""
)

data class FamilyDecisionRequest(
    val status: String = "family_review",
    @SerializedName("familyVote") val familyVote: String = "discuss",
    val note: String = "",
    @SerializedName("nextStep") val nextStep: String = "",
    @SerializedName("nextStepAt") val nextStepAt: String? = null
)

data class FamilyDecisionCommentRequest(
    val vote: String = "discuss",
    val comment: String = ""
)

data class FamilyDecisionCommentData(
    @SerializedName("familyCommentId") val familyCommentId: String = "",
    val vote: String = "discuss",
    val comment: String = "",
    @SerializedName("createdAt") val createdAt: String = ""
)

data class FamilyDecisionData(
    @SerializedName("familyDecisionId") val familyDecisionId: String = "",
    @SerializedName("ownerProfileId") val ownerProfileId: String = "",
    @SerializedName("targetProfileId") val targetProfileId: String = "",
    val status: String = "considering",
    @SerializedName("familyVote") val familyVote: String = "discuss",
    val note: String = "",
    @SerializedName("nextStep") val nextStep: String = "",
    @SerializedName("nextStepAt") val nextStepAt: String? = null,
    @SerializedName("updatedAt") val updatedAt: String = "",
    @SerializedName("targetName") val targetName: String = "Member",
    @SerializedName("targetAge") val targetAge: Int = 0,
    @SerializedName("targetLocation") val targetLocation: String = "",
    @SerializedName("targetOccupation") val targetOccupation: String = "",
    @SerializedName("targetPhotoUrl") val targetPhotoUrl: String? = null,
    @SerializedName("isVerified") val isVerified: Boolean = false,
    @SerializedName("trustScore") val trustScore: Int = 0,
    @SerializedName("trustLevel") val trustLevel: String = "low",
    @SerializedName("trustSignals") val trustSignals: List<String> = emptyList(),
    @SerializedName("trustFactors") val trustFactors: List<TrustFactorData> = emptyList(),
    val comments: List<FamilyDecisionCommentData> = emptyList()
)

data class SearchResultsData(
    val results: List<SearchProfileItem> = emptyList(),
    val total: Int = 0,
    val page: Int = 1,
    val limit: Int = 20
)

data class SavedSearchData(
    @SerializedName("searchId") val searchId: String = "",
    val label: String = "",
    val ageMin: Int? = null,
    val ageMax: Int? = null,
    val religion: String? = null,
    val city: String? = null
)

data class InterestRequest(val receiverId: String)
data class RespondRequest(val status: String)

data class InterestResult(
    val status: String = "",
    val interestId: String = "",
    val isMutual: Boolean = false
)

data class NotificationData(
    val notificationId: String = "",
    val title: String = "",
    val body: String = "",
    val data: Map<String, String> = emptyMap(),
    val status: String = "queued",
    val createdAt: String = "",
    val readAt: String? = null
)

data class InterestListItem(
    @SerializedName("interest_id") val interestId: String = "",
    @SerializedName("profile_id") val profileId: String = "",
    @SerializedName("user_id") val userId: String = "",
    @SerializedName("first_name") val firstName: String = "",
    @SerializedName("last_name") val lastName: String = "",
    @SerializedName("primary_photo_url") val primaryPhotoUrl: String? = null,
    val occupation: String = "",
    @SerializedName("working_city") val workingCity: String = "",
    @SerializedName("family_city") val familyCity: String = "",
    val status: String = "pending",
    @SerializedName("sent_at") val sentAt: String = ""
)

data class ShortlistItem(
    @SerializedName("profile_id") val profileId: String = "",
    @SerializedName("user_id") val userId: String = "",
    @SerializedName("first_name") val firstName: String = "",
    @SerializedName("last_name") val lastName: String = "",
    @SerializedName("primary_photo_url") val primaryPhotoUrl: String? = null,
    @SerializedName("added_at") val addedAt: String = ""
)

data class ActionResult(val action: String = "")

data class PlanData(
    val planId: String = "",
    val name: String = "",
    val displayName: String = "",
    val price: Int = 0,
    val duration: String = "",
    val durationDays: Int = 0,
    val tierRank: Int = 0,
    val features: List<String> = emptyList()
)

data class OrderRequest(val planId: String)

data class OrderData(
    val orderId: String = "",
    val amount: Int = 0,
    val currency: String = "INR",
    val planId: String = "",
    val gateway: String = "razorpay",
    val keyId: String = ""
)

data class PaymentVerifyRequest(
    val orderId: String,
    val paymentId: String,
    val signature: String,
    val planId: String
)

data class SubscriptionData(
    @SerializedName("plan_id") val planId: String = "free",
    @SerializedName("effective_plan_id") val effectivePlanId: String = "bronze",
    @SerializedName("is_active") val isActive: Boolean = true,
    @SerializedName("start_date") val startDate: String? = null,
    @SerializedName("end_date") val endDate: String? = null,
    @SerializedName("plan_name") val planName: String = "",
    @SerializedName("duration_days") val durationDays: Int? = null,
    val entitlements: MemberPlanEntitlements = MemberPlanEntitlements(),
    val usage: MemberSubscriptionUsageData = MemberSubscriptionUsageData()
)

data class MemberPlanEntitlements(
    val planId: String = "bronze",
    val label: String = "Bronze",
    val visibleMatches: Int = 80,
    val profileViews: Int = 10,
    val contactDetails: Int = 0,
    val engagePlus: Boolean = false,
    val shortlist: Int = 5,
    val interests: Int = 5,
    val matchAssistance: Boolean = false,
    val chat: Boolean = false,
    val spotlightBoosts: Int = 0
)

data class MemberSubscriptionUsageData(
    @SerializedName("period_started_at") val periodStartedAt: String? = null,
    @SerializedName("period_ends_at") val periodEndsAt: String? = null,
    @SerializedName("profile_views_used") val profileViewsUsed: Int = 0,
    @SerializedName("contact_unlocks_used") val contactUnlocksUsed: Int = 0,
    @SerializedName("shortlists_used") val shortlistsUsed: Int = 0,
    @SerializedName("interests_used") val interestsUsed: Int = 0,
    @SerializedName("spotlight_boosts_used") val spotlightBoostsUsed: Int = 0
)

data class BrandingConfig(
    val appTitle: String = "SoulMatch",
    val tagline: String = "Serious matchmaking for modern families",
    val logoUrl: String = "",
    val squareLogoUrl: String = "",
    val previewImageUrl: String = "",
    val shareBaseUrl: String = ""
)

data class ThemeConfig(
    val primary: String = "#FF5C00",
    val secondary: String = "#FF8533",
    val accent: String = "#16324F",
    val background: String = "#FFFFFF",
    val surface: String = "#FFFFFF"
)

data class FeatureFlagsData(
    val chat: Boolean = true,
    val videoCalling: Boolean = true,
    val maintenanceMode: Boolean = false
)

data class MaintenanceConfig(
    val enabled: Boolean = false,
    val title: String = "Scheduled maintenance",
    val message: String = "We are tuning SoulMatch for a smoother experience. Please check back shortly.",
    val startsAt: String? = null,
    val endsAt: String? = null
)

data class LegalSectionData(
    val heading: String = "",
    val body: String = ""
)

data class LegalDocumentData(
    val title: String = "",
    val subtitle: String = "",
    val updatedAt: String = "",
    val sections: List<LegalSectionData> = emptyList()
)

data class LegalContentData(
    val terms: LegalDocumentData = defaultSoulMatchTerms(),
    val privacy: LegalDocumentData = defaultSoulMatchPrivacy()
)

data class AuthContentData(
    val heroTitle: String = "Serious matchmaking for modern families",
    val heroSubtitle: String = "Serious matchmaking for modern families",
    val trustChips: List<String> = listOf("Verified profiles", "Private photos", "Family-ready"),
    val registerCta: String = "Register with mobile",
    val googleCta: String = "Continue with Google",
    val loginCta: String = "Log in to existing account",
    val termsPrefix: String = "By continuing, you agree to our"
)

data class PhoneEntryContentData(
    val topBarTitle: String = "Mobile verification",
    val title: String = "Enter your mobile number",
    val subtitle: String = "We use this number for OTP login, account recovery, and important match alerts.",
    val trustLines: List<String> = listOf("No password needed", "Private by default"),
    val fieldLabel: String = "10 digit mobile number",
    val helperText: String = "Use your active Indian mobile number.",
    val privacyTitle: String = "Your number stays protected",
    val privacyBody: String = "Members see contact details only when your privacy and plan rules allow it.",
    val submitCta: String = "Send OTP"
)

data class HomeBestMatchAdData(
    val id: String = "",
    val type: String = "marriage",
    val enabled: Boolean = true,
    val targetPlans: List<String> = emptyList(),
    val minPlan: String = "",
    val maxPlan: String = "",
    val theme: String = "",
    val badge: String = "",
    val title: String = "",
    val body: String = "",
    val bullets: List<String> = emptyList(),
    val cta: String = "Explore",
    @SerializedName("discountLabel") val discountLabel: String = "",
    @SerializedName("imageUrl") val imageUrl: String = "",
    val destination: String = "search"
)

data class HomeScamAwarenessCardData(
    val id: String = "",
    val enabled: Boolean = true,
    val title: String = "",
    val body: String = "",
    val illustration: String = ""
)

data class HomeContentData(
    val eyebrow: String = "SoulMatch",
    val headerSubtitle: String = "Premium matrimonial matches ranked by family fit, intent, activity, and compatibility.",
    val upgradeTitle: String = "Unlock contact details and premium visibility",
    val upgradeDetail: String = "Upgrade to view verified phone numbers, get more profile reach, and continue high-intent conversations.",
    val bestMatchesTitle: String = "Best matches",
    val bestMatchesSubtitle: String = "High-signal cards with interest, shortlist, profile, and more actions",
    val bestMatchMinimumProfiles: Int = 5,
    val bestMatchHighCompatibilityThreshold: Int = 80,
    val bestMatchInsertFrequency: Int = 2,
    val showBestMatchInsertCards: Boolean = true,
    val showBestMatchUpgradeCards: Boolean = true,
    val showBestMatchAdCards: Boolean = true,
    val bestMatchAdCards: List<HomeBestMatchAdData> = emptyList(),
    val scamAwarenessCards: List<HomeScamAwarenessCardData> = emptyList(),
    val emptyTitle: String = "Your profile needs a little more detail",
    val emptyBody: String = "Complete career, family, lifestyle, and privacy sections to unlock stronger recommendations.",
    val emptyCta: String = "Improve my profile",
    val searchPlaceholder: String = "Search city, community, education, or profession",
    val shortlistHint: String = "shortlisted from current filters. Saved in Activity > Saved.",
    val filterTitle: String = "Filters",
    val filterSubtitle: String = "Any age, any location, any community"
)

fun defaultHomeBestMatchAds(): List<HomeBestMatchAdData> = listOf(
    HomeBestMatchAdData(
        id = "upgrade-bronze-to-silver",
        type = "upgrade",
        targetPlans = listOf("free", "bronze"),
        minPlan = "free",
        maxPlan = "free",
        theme = "rose",
        badge = "Starter upgrade",
        title = "Move from Bronze to Pro",
        body = "Unlock contact sharing, Engage+, and contact details for serious conversations.",
        bullets = listOf(
            "25 contact details",
            "Engage+ insights",
            "Gold badge",
            "Safer introductions"
        ),
        cta = "Upgrade to Pro",
        discountLabel = "PRO BENEFITS",
        destination = "membership"
    ),
    HomeBestMatchAdData(
        id = "upgrade-silver-to-gold",
        type = "upgrade",
        targetPlans = listOf("free", "bronze", "silver"),
        minPlan = "free",
        maxPlan = "silver",
        theme = "gold",
        badge = "Recommended",
        title = "The Pro Max experience",
        body = "Get stronger contact limits, Super Interests, and monthly spotlight visibility.",
        bullets = listOf("50 contact details", "50 Super Interests", "1 spotlight", "Priority reach"),
        cta = "Explore Pro Max",
        discountLabel = "MOST CHOSEN",
        destination = "membership"
    ),
    HomeBestMatchAdData(
        id = "upgrade-gold-to-platinum",
        type = "upgrade",
        targetPlans = listOf("free", "bronze", "silver", "gold"),
        minPlan = "free",
        maxPlan = "gold",
        theme = "dark",
        badge = "Elite access",
        title = "Pro Supreme for families who want full access",
        body = "Unlock the highest contact limit, more Super Interests, and stronger spotlight reach.",
        bullets = listOf("80 contact details", "80 Super Interests", "3 spotlights", "Top seller access"),
        cta = "Go Pro Supreme",
        discountLabel = "PRO SUPREME",
        destination = "membership"
    ),
    HomeBestMatchAdData(
        id = "spotlight-day-pass",
        type = "spotlight",
        targetPlans = listOf("free", "bronze", "silver", "gold"),
        minPlan = "free",
        maxPlan = "gold",
        theme = "sunrise",
        badge = "Spotlight",
        title = "Be the first profile others see for an entire day",
        body = "Appear on top of recommendations and increase your chances of receiving more interests.",
        cta = "Get Spotlight",
        destination = "membership"
    ),
    HomeBestMatchAdData(
        id = "contact-unlock",
        type = "membership",
        targetPlans = listOf("free", "bronze", "silver"),
        minPlan = "free",
        maxPlan = "silver",
        theme = "blue",
        badge = "Contact access",
        title = "Ready to speak with the right family?",
        body = "Upgrade to unlock eligible contact views after privacy and trust checks.",
        bullets = listOf("Verified phone access", "Privacy-first contact rules", "Safer introductions"),
        cta = "Unlock contacts",
        destination = "membership"
    ),
    HomeBestMatchAdData(
        id = "profile-boost",
        type = "membership",
        targetPlans = listOf("free", "bronze", "silver"),
        minPlan = "free",
        maxPlan = "silver",
        theme = "rose",
        badge = "Boost",
        title = "Get noticed by more compatible families",
        body = "Boosted profiles receive higher placement in compatible recommendations.",
        bullets = listOf("Higher listing priority", "More profile views", "Better response chances"),
        cta = "Boost my profile",
        destination = "membership"
    ),
    HomeBestMatchAdData(
        id = "horoscope-family-match",
        type = "astrology",
        targetPlans = listOf("free", "bronze", "silver", "gold", "platinum"),
        theme = "purple",
        badge = "Horoscope",
        title = "Add horoscope details for family compatibility",
        body = "Help families compare birth details, rashi, nakshatra, and kundli expectations.",
        bullets = listOf("Kundli details improve family fit", "Manglik and rashi checks stay clear", "Useful before a family call"),
        cta = "Open astrology",
        destination = "astrology_services"
    ),
    HomeBestMatchAdData(
        id = "verified-trust-profile",
        type = "trust",
        targetPlans = listOf("free", "bronze", "silver", "gold", "platinum"),
        theme = "green",
        badge = "Trust profile",
        title = "Verified profiles receive more confident responses",
        body = "Complete phone, email, photo, document, education, income, and family trust checks.",
        bullets = listOf("Higher trust score", "More confident family responses", "Verification status stays visible"),
        cta = "Improve trust",
        destination = "my_profile"
    ),
    HomeBestMatchAdData(
        id = "enable-notifications",
        type = "notification",
        targetPlans = listOf("free", "bronze", "silver", "gold", "platinum"),
        theme = "blue",
        badge = "Alerts",
        title = "Turn on match alerts",
        body = "Get notified when a serious profile sends interest, accepts, or messages you.",
        bullets = listOf("New interest alerts", "Acceptance reminders", "Message notifications"),
        cta = "Manage alerts",
        destination = "settings"
    ),
    HomeBestMatchAdData(
        id = "private-photo-control",
        type = "privacy",
        targetPlans = listOf("free", "bronze", "silver", "gold", "platinum"),
        theme = "ivory",
        badge = "Privacy",
        title = "Keep photos private until you are ready",
        body = "Use request-based photo access so families can review visibility safely.",
        cta = "Manage photos",
        destination = "my_profile"
    ),
    HomeBestMatchAdData(
        id = "assisted-discovery",
        type = "assist",
        targetPlans = listOf("silver", "gold", "platinum"),
        minPlan = "silver",
        theme = "peach",
        badge = "SoulMatch Assist",
        title = "Need offline help from a local agent?",
        body = "Share your profile with selected registered agents for offline introductions.",
        cta = "Open Assist",
        destination = "soulmatch_assist"
    ),
    HomeBestMatchAdData(
        id = "wedding-readiness",
        type = "marriage",
        targetPlans = listOf("free", "bronze", "silver", "gold", "platinum"),
        theme = "maroon",
        badge = "Family planning",
        title = "Shortlist services after both families connect",
        body = "Keep venues, photography, and ceremony planning separate from discovery until you are ready.",
        cta = "View ideas",
        destination = "search"
    ),
    HomeBestMatchAdData(
        id = "success-stories",
        type = "story",
        targetPlans = listOf("free", "bronze", "silver", "gold", "platinum"),
        theme = "cream",
        badge = "Success stories",
        title = "See how families used SoulMatch safely",
        body = "Browse real journey patterns and learn what details create better responses.",
        cta = "View stories",
        destination = "success_stories"
    ),
    HomeBestMatchAdData(
        id = "safety-awareness",
        type = "safety",
        targetPlans = listOf("free", "bronze", "silver", "gold", "platinum"),
        theme = "cream",
        badge = "Scam awareness",
        title = "Protect yourself from online frauds",
        body = "Simple safety reminders for every serious matchmaking journey.",
        cta = "Safety centre",
        destination = "safety"
    )
)

fun defaultScamAwarenessCards(): List<HomeScamAwarenessCardData> = listOf(
    HomeScamAwarenessCardData(
        id = "gift-cod",
        title = "Never make payments for unsolicited gifts",
        body = "Scammers may send gifts by cash-on-delivery and pressure you to pay."
    ),
    HomeScamAwarenessCardData(
        id = "import-duty",
        title = "Do not pay import duty or custom fees",
        body = "Fraudsters may pose as officials and demand fees for gifts or parcels."
    ),
    HomeScamAwarenessCardData(
        id = "video-call",
        title = "Be cautious during video calls",
        body = "Avoid explicit calls and report anyone who blackmails or asks for money."
    ),
    HomeScamAwarenessCardData(
        id = "emergency-cash",
        title = "Validate emergency cash requests",
        body = "Never transfer money because of sudden medical, travel, or family emergencies."
    ),
    HomeScamAwarenessCardData(
        id = "advance-fee",
        title = "Agents must not demand advance fees",
        body = "Use SoulMatch-listed agents carefully and report anyone asking for unofficial payments."
    ),
    HomeScamAwarenessCardData(
        id = "bank-transfer",
        title = "Avoid direct bank transfers to new contacts",
        body = "Do not send money for visas, tickets, gifts, loans, medical stories, or emergencies."
    )
)

data class NavigationContentData(
    val home: String = "Home",
    val search: String = "Search",
    val activity: String = "Activity",
    val chat: String = "Messenger",
    val profile: String = "Profile",
    val upgrade: String = "Upgrade"
)

data class SupportContentData(
    val title: String = "Need help?",
    val body: String = "Contact SoulMatch support from settings for account, safety, payment, or privacy help.",
    val email: String = "support@soulmatch.app"
)

data class NotificationPromptContentData(
    val enabled: Boolean = true,
    val title: String = "Don't miss important match updates",
    val subtitle: String = "Turn on alerts for interests, acceptances, and family messages.",
    val bullets: List<String> = listOf(
        "Get alerts when a new interest or recommendation arrives",
        "Know when a family accepts your interest",
        "Be notified when a profile sends you a message"
    ),
    val allowCta: String = "Allow notifications",
    val laterCta: String = "Maybe later"
)

data class SafetyCenterTileData(
    val id: String = "",
    val title: String = "",
    val subtitle: String = "",
    val icon: String = "shield",
    val tone: String = "gold",
    val destination: String = ""
)

data class SafetyCenterContactData(
    val label: String = "",
    val value: String = "",
    val type: String = "text"
)

data class SafetyCenterArticleData(
    val id: String = "",
    val title: String = "",
    val subtitle: String = "",
    val body: String = "",
    val bullets: List<String> = emptyList(),
    val contacts: List<SafetyCenterContactData> = emptyList(),
    val primaryCta: String = "",
    val destination: String = ""
)

data class SafetyCenterResourceData(
    val id: String = "",
    val title: String = "",
    val subtitle: String = "",
    val icon: String = "help",
    val destination: String = ""
)

data class SafetyCenterVerificationCardData(
    val title: String = "Help us make SoulMatch safe and authentic",
    val body: String = "Verified profiles help families move forward with more confidence. Complete your trust details when you are ready.",
    val cta: String = "Verify yourself",
    val destination: String = "my_profile"
)

data class SafetyCenterContentData(
    val title: String = "Safety Center",
    val subtitle: String = "Explore practical tools and guidance that help you stay safe while matching.",
    val tiles: List<SafetyCenterTileData> = listOf(
        SafetyCenterTileData(
            id = "online_personal_tips",
            title = "Online / Personal Tips",
            subtitle = "Safe habits before sharing personal details.",
            icon = "tips",
            tone = "gold",
            destination = "article:online_personal_tips"
        ),
        SafetyCenterTileData(
            id = "privacy_settings",
            title = "Privacy Settings",
            subtitle = "Control photos, alerts, and visibility.",
            icon = "shield",
            tone = "green",
            destination = "article:privacy_settings"
        ),
        SafetyCenterTileData(
            id = "report_block",
            title = "Report / Block Profile",
            subtitle = "Act quickly on suspicious behavior.",
            icon = "report",
            tone = "rose",
            destination = "article:report_block"
        ),
        SafetyCenterTileData(
            id = "mental_wellbeing",
            title = "Mental Wellbeing",
            subtitle = "Move at a pace that feels right.",
            icon = "heart",
            tone = "purple",
            destination = "article:mental_wellbeing"
        )
    ),
    val verificationCard: SafetyCenterVerificationCardData = SafetyCenterVerificationCardData(),
    val resourcesTitle: String = "We're here for you",
    val resources: List<SafetyCenterResourceData> = listOf(
        SafetyCenterResourceData(
            id = "cyber_crime",
            title = "Other resources",
            subtitle = "Cyber cell contacts to help you take action.",
            icon = "help",
            destination = "article:cyber_crime"
        )
    ),
    val articles: List<SafetyCenterArticleData> = listOf(
        SafetyCenterArticleData(
            id = "online_personal_tips",
            title = "Online / Personal Tips",
            subtitle = "Choose Online Safety, Fraud Protection, or Personal Meeting guidance before moving ahead.",
            body = "Safety guidance is split into practical sections so families can review the right caution at the right moment."
        ),
        SafetyCenterArticleData(
            id = "privacy_settings",
            title = "Privacy Settings",
            subtitle = "Choose how much of your profile is visible while you are still deciding.",
            bullets = listOf(
                "Use private photos when you want matches to request access first.",
                "Pause push notifications when you do not want alerts on this device.",
                "Hide or block profiles when you do not want further interaction."
            ),
            primaryCta = "Open privacy settings",
            destination = "settings"
        ),
        SafetyCenterArticleData(
            id = "report_block",
            title = "Report / Block Profile",
            subtitle = "Report suspicious requests, abusive messages, fake profiles, or money demands.",
            bullets = listOf(
                "How to identify possible violations: pressure for secrecy, abusive language, fake claims, payment requests, or document misuse.",
                "Ways to take action: preserve screenshots, stop private sharing, and keep conversations inside SoulMatch while reviewing.",
                "Report the profile when there is fraud, harassment, impersonation, explicit content, or unsafe behavior.",
                "Block the profile immediately when you do not want further calls, messages, or visibility."
            )
        ),
        SafetyCenterArticleData(
            id = "mental_wellbeing",
            title = "Mental Wellbeing",
            subtitle = "Matchmaking can be emotional. Keep the process steady and family-supported.",
            bullets = listOf(
                "Take breaks from discovery when conversations feel overwhelming.",
                "Avoid pressure to decide quickly or share details before you are ready.",
                "Use trusted family support for important decisions."
            )
        ),
        SafetyCenterArticleData(
            id = "cyber_crime",
            title = "Take action against cyber crime",
            subtitle = "Use official channels to report illegal online activity.",
            body = "If a profile threatens, blackmails, impersonates, or asks for money, preserve screenshots and report through official cyber crime channels.",
            contacts = listOf(
                SafetyCenterContactData("National cyber crime helpline", "1930", "phone"),
                SafetyCenterContactData("Cyber crime website", "https://cybercrime.gov.in/", "url")
            )
        )
    )
)

data class ClientIntegrationsData(
    val googleWebClientId: String = "",
    val razorpayKeyId: String = "",
    val supportEmail: String = "support@soulmatch.app"
)

data class AppContentData(
    val auth: AuthContentData = AuthContentData(),
    val phoneEntry: PhoneEntryContentData = PhoneEntryContentData(),
    val home: HomeContentData = HomeContentData(),
    val navigation: NavigationContentData = NavigationContentData(),
    val support: SupportContentData = SupportContentData(),
    val notificationPrompt: NotificationPromptContentData = NotificationPromptContentData(),
    val safetyCenter: SafetyCenterContentData = SafetyCenterContentData()
)

data class RuntimeConfigData(
    val branding: BrandingConfig = BrandingConfig(),
    val theme: ThemeConfig = ThemeConfig(),
    val features: FeatureFlagsData = FeatureFlagsData(),
    val maintenance: MaintenanceConfig = MaintenanceConfig(),
    val monetization: MonetizationRuntimeData = MonetizationRuntimeData(),
    val content: AppContentData = AppContentData(),
    val legal: LegalContentData = LegalContentData(),
    val clientIntegrations: ClientIntegrationsData = ClientIntegrationsData()
)

data class MonetizationRuntimeData(
    val currency: String = "INR",
    val accessMode: String = "subscription",
    val subscriptionModelEnabled: Boolean = true,
    val fixedPriceAmount: Int = 200,
    val fixedPricePlanId: String = "fixed_access",
    val fixedPriceLabel: String = "₹200",
    val freeAccessLabel: String = "Account",
    val refundGuaranteeEnabled: Boolean = true,
    val refundGuaranteeTitle: String = "30-day full refund guarantee*",
    val refundGuaranteeSubtitle: String = "*Conditions apply",
    val premiumLimits: Map<String, Map<String, Int>> = emptyMap(),
    val plans: List<PlanData> = emptyList(),
    val memberPlanEntitlements: Map<String, MemberPlanEntitlements> = emptyMap(),
    val membershipFeatureMatrix: List<Map<String, Any>> = emptyList()
)

fun defaultSoulMatchTerms(): LegalDocumentData = LegalDocumentData(
    title = "Terms of Service",
    subtitle = "Simple rules for using SoulMatch safely and respectfully.",
    updatedAt = "29 Apr 2026",
    sections = listOf(
        LegalSectionData(
            heading = "Who can use SoulMatch",
            body = "SoulMatch is for lawful matrimonial discovery. You must be legally eligible to marry in India and use your own real identity, phone number, and profile details."
        ),
        LegalSectionData(
            heading = "Profile information",
            body = "Add correct details about age, education, job, community, family, photos, and preferences. Do not create fake profiles, impersonate another person, or upload misleading photos."
        ),
        LegalSectionData(
            heading = "Respectful communication",
            body = "Use chats, interests, calls, and profile actions respectfully. Harassment, abusive messages, spam, fraud, money requests, and pressure tactics are not allowed."
        ),
        LegalSectionData(
            heading = "Safety actions",
            body = "You can hide, block, or report a member at any time. SoulMatch may review reports, restrict accounts, remove content, or contact users when safety checks are needed."
        ),
        LegalSectionData(
            heading = "Membership and payments",
            body = "Paid plans unlock features such as contact views, visibility, and premium profile actions. Plan benefits, price, validity, taxes, and refund rules are shown before payment."
        ),
        LegalSectionData(
            heading = "Account responsibility",
            body = "Keep your phone, OTP, and account access private. You are responsible for activity from your account unless you report unauthorized access promptly."
        ),
        LegalSectionData(
            heading = "Service changes",
            body = "SoulMatch may improve, pause, or change features to keep the service reliable and safe. Important changes will be shown in the app or through official communication."
        ),
        LegalSectionData(
            heading = "Need help",
            body = "For account, payment, privacy, or safety questions, contact SoulMatch support from the app settings page."
        )
    )
)

fun defaultSoulMatchPrivacy(): LegalDocumentData = LegalDocumentData(
    title = "Privacy Policy",
    subtitle = "How SoulMatch uses your information to help you find suitable matches.",
    updatedAt = "29 Apr 2026",
    sections = listOf(
        LegalSectionData(
            heading = "Information we collect",
            body = "We collect details you add to your profile, such as name, age, gender, language, community, education, job, family details, photos, preferences, and verification information."
        ),
        LegalSectionData(
            heading = "How we use it",
            body = "We use your information to create your profile, recommend matches, show compatibility signals, run search filters, verify members, prevent misuse, provide support, and offer optional profile-writing and ice-breaker suggestions."
        ),
        LegalSectionData(
            heading = "Who can see your profile",
            body = "Your visibility and photo privacy settings decide who can see profile details and photos. You can hide or block members when you do not want further interaction."
        ),
        LegalSectionData(
            heading = "Chats and safety",
            body = "Messages, interests, reports, and safety actions may be processed through rule-based and optional AI-assisted checks to protect members, investigate complaints, block risky requests, and improve trust on the platform."
        ),
        LegalSectionData(
            heading = "Payments",
            body = "Payment details are handled by authorized payment partners. SoulMatch stores payment status, plan details, invoice data, and transaction references needed for service and support."
        ),
        LegalSectionData(
            heading = "Sharing with partners",
            body = "We share limited information with trusted service providers such as hosting, analytics, payment, notification, support, and configured AI providers only for operating SoulMatch features and safety checks."
        ),
        LegalSectionData(
            heading = "Your choices",
            body = "You can edit profile details, manage photos, change privacy settings, delete photos, hide members, block members, and ask for account support from the app."
        ),
        LegalSectionData(
            heading = "Data security",
            body = "We use access controls, verification, and monitoring to protect member data. No online service is risk free, so always be careful before sharing personal or financial information."
        )
    )
)

data class InvoiceItem(
    @SerializedName("transaction_id") val transactionId: String = "",
    @SerializedName("created_at") val createdAt: String = "",
    val amount: Double = 0.0,
    val currency: String = "INR",
    val status: String = "",
    val gateway: String = "",
    @SerializedName("payment_method") val paymentMethod: String = "",
    @SerializedName("payment_instrument") val paymentInstrument: String = "",
    @SerializedName("provider_status") val providerStatus: String = "",
    @SerializedName("razorpay_order_id") val razorpayOrderId: String = "",
    @SerializedName("razorpay_payment_id") val razorpayPaymentId: String = "",
    @SerializedName("payment_order_id") val paymentOrderId: String = "",
    @SerializedName("payment_order_status") val paymentOrderStatus: String = "",
    @SerializedName("subscription_id") val subscriptionId: String = "",
    @SerializedName("plan_id") val planId: String = "",
    @SerializedName("plan_name") val planName: String = "",
    @SerializedName("start_date") val startDate: String = "",
    @SerializedName("end_date") val endDate: String = "",
    @SerializedName("is_active") val isActive: Boolean = false,
    @SerializedName("duration_days") val durationDays: Int? = null
)

data class BotFlowMessage(
    @SerializedName("flowStepId") val flowStepId: String = "",
    val alias: String = "",
    val content: String = "",
    @SerializedName("messageType") val messageType: String = "text",
    @SerializedName("messageUserType") val messageUserType: String = "bot",
    @SerializedName("messageUserAlias") val messageUserAlias: String = "SoulMatch Assist",
    @SerializedName("createdMillis") val createdMillis: Long = 0L
)

data class ChatMessage(
    val messageId: String = "",
    val chatId: String = "",
    val senderId: String = "",
    val receiverId: String = "",
    val type: String = "text",
    val content: String = "",
    val status: String = "sent",
    val sentAt: String = "",
    @SerializedName("flowStepId") val flowStepId: String? = null,
    val alias: String? = null,
    @SerializedName("messageType") val messageType: String? = null,
    @SerializedName("messageUserType") val messageUserType: String? = null,
    @SerializedName("messageUserAlias") val messageUserAlias: String? = null,
    @SerializedName("createdMillis") val createdMillis: Long? = null
)

data class ChatLastMessage(
    val content: String = "",
    val type: String = "text",
    val sentAt: String = "",
    val senderId: String = ""
)

data class ConversationItem(
    val chatId: String = "",
    val participants: List<String> = emptyList(),
    val participantUserId: String = "",
    val participantProfileId: String = "",
    val participantName: String = "",
    val participantPhotoUrl: String? = null,
    val lastMessage: ChatLastMessage? = null,
    val unreadCounts: Map<String, Int> = emptyMap(),
    val updatedAt: String = "",
    val flowId: String? = null,
    val flowVersionId: String? = null,
    val flowMessages: List<BotFlowMessage> = emptyList(),
    val flowMessagesJson: String? = null,
    val flowBusinessHourType: String? = null,
    val serviceAccountId: String? = null,
    val operatingHoursId: String? = null
)

data class MessagePageData(
    val messages: List<ChatMessage> = emptyList(),
    val page: Int = 1,
    val limit: Int = 50
)

data class ChatEligibilityData(
    val canChat: Boolean = false,
    val reason: String = ""
)

private fun safeString(value: String?): String = value.orEmpty()

fun ProfileData.fullName(): String = listOf(safeString(firstName), safeString(lastName)).filter { it.isNotBlank() }.joinToString(" ").ifBlank { "Member" }

fun InterestListItem.fullName(): String = listOf(safeString(firstName), safeString(lastName)).filter { it.isNotBlank() }.joinToString(" ").ifBlank { "Member" }

fun ShortlistItem.fullName(): String = listOf(safeString(firstName), safeString(lastName)).filter { it.isNotBlank() }.joinToString(" ").ifBlank { "Member" }

fun ViewerData.fullName(): String = listOf(safeString(firstName), safeString(lastName)).filter { it.isNotBlank() }.joinToString(" ").ifBlank { "Member" }

fun PhotoAccessRequestData.fullName(): String = listOf(safeString(firstName), safeString(lastName)).filter { it.isNotBlank() }.joinToString(" ").ifBlank { "Member" }

fun SearchProfileItem.toProfileSummary(seed: ProfileSummary? = null): ProfileSummary {
    val resolvedName = listOf(safeString(firstName), safeString(lastName)).filter { it.isNotBlank() }.joinToString(" ").ifBlank { seed?.name ?: "Member" }
    return ProfileSummary(
        profileId = profileId,
        userId = if (safeString(userId).isNotBlank()) safeString(userId) else seed?.userId.orEmpty(),
        name = resolvedName,
        age = if (age > 0) age else seed?.age ?: 0,
        gender = if (safeString(gender).isNotBlank()) safeString(gender) else seed?.gender.orEmpty(),
        location = if (safeString(workingCity).isNotBlank()) safeString(workingCity) else seed?.location.orEmpty(),
        occupation = if (safeString(occupation).isNotBlank()) safeString(occupation) else seed?.occupation.orEmpty(),
        primaryPhoto = primaryPhotoUrl ?: seed?.primaryPhoto,
        compatibilityScore = seed?.compatibilityScore ?: 72,
        compatibilityBreakdown = seed?.compatibilityBreakdown,
        heightCm = heightCm ?: seed?.heightCm,
        isVerified = isVerified || seed?.isVerified == true,
        isPhotoPrivate = isPhotoPrivate || seed?.isPhotoPrivate == true,
        trustScore = if (trustScore > 0) trustScore else seed?.trustScore ?: 0,
        trustLevel = if (safeString(trustLevel).isNotBlank()) safeString(trustLevel) else seed?.trustLevel ?: "low",
        trustSignals = if (trustSignals.isNotEmpty()) trustSignals else seed?.trustSignals ?: emptyList(),
        trustFactors = if (trustFactors.isNotEmpty()) trustFactors else seed?.trustFactors ?: emptyList(),
        education = if (safeString(educationLevel).isNotBlank()) safeString(educationLevel) else seed?.education.orEmpty(),
        community = when {
            safeString(caste).isNotBlank() -> safeString(caste)
            safeString(religion).isNotBlank() -> safeString(religion)
            else -> seed?.community.orEmpty()
        },
        religion = if (safeString(religion).isNotBlank()) safeString(religion) else seed?.religion.orEmpty(),
        annualIncome = if (safeString(annualIncome).isNotBlank()) safeString(annualIncome) else seed?.annualIncome.orEmpty(),
        familyCity = if (safeString(familyCity).isNotBlank()) safeString(familyCity) else seed?.familyCity.orEmpty(),
        familyState = if (safeString(familyState).isNotBlank()) safeString(familyState) else seed?.familyState.orEmpty(),
        maritalStatus = if (safeString(maritalStatus).isNotBlank()) safeString(maritalStatus) else seed?.maritalStatus.orEmpty(),
        diet = if (safeString(diet).isNotBlank()) safeString(diet) else seed?.diet.orEmpty(),
        isManglik = isManglik || seed?.isManglik == true,
        createdAt = if (safeString(createdAt).isNotBlank()) safeString(createdAt) else seed?.createdAt.orEmpty(),
        hideLastSeen = hideLastSeen || seed?.hideLastSeen == true,
        lastActiveLabel = lastActiveLabelSnake.ifBlank { seed?.lastActiveLabel ?: "Recently Active" },
        matchReasons = if (matchReasons.isNotEmpty()) matchReasons else seed?.matchReasons ?: emptyList(),
        interestSent = seed?.interestSent ?: false,
        shortlisted = seed?.shortlisted ?: false,
        profileCreatedBy = if (safeString(profileCreatedBy).isNotBlank()) safeString(profileCreatedBy) else seed?.profileCreatedBy ?: "self"
    )
}
