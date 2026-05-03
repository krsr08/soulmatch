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

data class SendOTPRequest(val phone: String)
data class VerifyOTPRequest(val phone: String, val otp: String)
data class GoogleLoginRequest(val googleToken: String)
data class FirebasePhoneLoginRequest(
    val firebaseToken: String,
    val phone: String? = null,
    val inviteCode: String? = null,
    val acquisitionSource: String? = null
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

data class AuthData(
    val accessToken: String,
    val refreshToken: String,
    val userId: String,
    val isNewUser: Boolean
)

data class ProfileStepResponse(
    val profileId: String = "",
    val completionScore: Int = 0,
    val step: Int = 1
)

data class PartnerPreferencesData(
    @SerializedName("age_min") val ageMin: Int = 24,
    @SerializedName("age_max") val ageMax: Int = 32,
    val religion: String? = null,
    @SerializedName("manglik_pref") val manglikPref: String = "any"
)

data class PrivacySettingsRequest(
    @SerializedName("photoPrivacy") val photoPrivacy: String,
    @SerializedName("profileVisibility") val profileVisibility: String
)

data class ProfileStatusRequest(
    @SerializedName("profileStatus") val profileStatus: String
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

data class AssistStatusRequest(
    @SerializedName("isOptedIn") val isOptedIn: Boolean,
    @SerializedName("supportLevel") val supportLevel: String,
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
    val reasons: List<String> = emptyList()
)

data class AssistReadinessData(
    @SerializedName("hasCity") val hasCity: Boolean = false,
    @SerializedName("hasPincode") val hasPincode: Boolean = false,
    @SerializedName("canAutoAssign") val canAutoAssign: Boolean = false
)

data class AssistStatusData(
    @SerializedName("profileId") val profileId: String = "",
    @SerializedName("isOptedIn") val isOptedIn: Boolean = false,
    @SerializedName("supportLevel") val supportLevel: String = "self_service",
    @SerializedName("requestStatus") val requestStatus: String = "not_requested",
    @SerializedName("preferredContactWindow") val preferredContactWindow: String = "",
    @SerializedName("familyContactName") val familyContactName: String = "",
    @SerializedName("familyContactPhone") val familyContactPhone: String = "",
    val notes: String = "",
    @SerializedName("assignedAt") val assignedAt: String? = null,
    @SerializedName("nextReviewAt") val nextReviewAt: String? = null,
    val location: AssistLocationData = AssistLocationData(),
    val readiness: AssistReadinessData = AssistReadinessData(),
    val advisor: AdvisorSummaryData? = null,
    val recommendations: List<AdvisorSummaryData> = emptyList()
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

data class ProfileData(
    @SerializedName("profile_id") val profileId: String = "",
    @SerializedName("user_id") val userId: String = "",
    @SerializedName("first_name") val firstName: String = "",
    @SerializedName("last_name") val lastName: String = "",
    val dob: String? = null,
    val age: Int = 0,
    val gender: String = "",
    val religion: String = "",
    val caste: String = "",
    @SerializedName("mother_tongue") val motherTongue: String = "",
    @SerializedName("marital_status") val maritalStatus: String = "",
    @SerializedName("completion_score") val completionScore: Int = 0,
    @SerializedName("is_partner_pref_set") val isPartnerPrefSet: Boolean = false,
    @SerializedName("profile_status") val profileStatus: String = "active",
    @SerializedName("profile_created_by") val profileCreatedBy: String = "self",
    @SerializedName("verification_status") val verificationStatus: String = "pending",
    @SerializedName("primary_photo_url") val primaryPhotoUrl: String? = null,
    @SerializedName("photo_privacy") val photoPrivacy: String = "all",
    @SerializedName("profile_visibility") val profileVisibility: String = "all",
    @SerializedName("education_level") val educationLevel: String = "",
    val occupation: String = "",
    @SerializedName("annual_income") val annualIncome: String = "",
    @SerializedName("working_city") val workingCity: String = "",
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
    val diet: String = "",
    val smoking: String = "",
    val drinking: String = "",
    @SerializedName("about_me") val aboutMe: String = "",
    val rashi: String = "",
    val nakshatra: String = "",
    @SerializedName("is_manglik") val isManglik: Boolean = false,
    @SerializedName("birth_city") val birthCity: String = "",
    val gotra: String = ""
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
    val location: String = "",
    val occupation: String = "",
    @SerializedName("primaryPhoto") val primaryPhoto: String? = null,
    @SerializedName("compatibilityScore") val compatibilityScore: Int = 0,
    @SerializedName("compatibilityBreakdown") val compatibilityBreakdown: CompatibilityBreakdown? = null,
    @SerializedName("heightCm") val heightCm: Int? = null,
    @SerializedName("isVerified") val isVerified: Boolean = false,
    @SerializedName("isPhotoPrivate") val isPhotoPrivate: Boolean = false,
    val education: String = "",
    val community: String = "",
    val lastActiveLabel: String = "Recently active",
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
    val religion: String = "",
    @SerializedName("primary_photo_url") val primaryPhotoUrl: String? = null,
    val occupation: String = "",
    @SerializedName("working_city") val workingCity: String = "",
    @SerializedName("education_level") val educationLevel: String = "",
    @SerializedName("annual_income") val annualIncome: String = "",
    @SerializedName("height_cm") val heightCm: Int? = null,
    val diet: String = "",
    @SerializedName("profile_created_by") val profileCreatedBy: String = "self"
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
    val price: Int = 0,
    val duration: String = "",
    val durationDays: Int = 0,
    val features: List<String> = emptyList()
)

data class OrderRequest(val planId: String)

data class OrderData(
    val orderId: String = "",
    val amount: Int = 0,
    val currency: String = "INR",
    val planId: String = "",
    val gateway: String = "razorpay"
)

data class PaymentVerifyRequest(
    val orderId: String,
    val paymentId: String,
    val signature: String,
    val planId: String
)

data class SubscriptionData(
    @SerializedName("plan_id") val planId: String = "free",
    @SerializedName("is_active") val isActive: Boolean = true,
    @SerializedName("start_date") val startDate: String? = null,
    @SerializedName("end_date") val endDate: String? = null
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
    val primary: String = "#D4285A",
    val secondary: String = "#F5A623",
    val accent: String = "#16324F",
    val background: String = "#FFF8F4",
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

data class HomeContentData(
    val eyebrow: String = "SoulMatch",
    val headerSubtitle: String = "Premium matrimonial matches ranked by family fit, intent, activity, and compatibility.",
    val upgradeTitle: String = "Unlock contact details and premium visibility",
    val upgradeDetail: String = "Upgrade to view verified phone numbers, get more profile reach, and continue high-intent conversations.",
    val bestMatchesTitle: String = "Best matches",
    val bestMatchesSubtitle: String = "High-signal cards with interest, shortlist, profile, and more actions",
    val emptyTitle: String = "Your profile needs a little more detail",
    val emptyBody: String = "Complete career, family, lifestyle, and privacy sections to unlock stronger recommendations.",
    val emptyCta: String = "Improve my profile",
    val searchPlaceholder: String = "Search city, community, education, or profession",
    val shortlistHint: String = "shortlisted from current filters. Saved in Activity > Saved.",
    val filterTitle: String = "Filters",
    val filterSubtitle: String = "Any age, any location, any community"
)

data class NavigationContentData(
    val home: String = "Home",
    val search: String = "Search",
    val activity: String = "Activity",
    val chat: String = "Chat",
    val profile: String = "Profile"
)

data class SupportContentData(
    val title: String = "Need help?",
    val body: String = "Contact SoulMatch support from settings for account, safety, payment, or privacy help.",
    val email: String = "support@soulmatch.app"
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
    val support: SupportContentData = SupportContentData()
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
    val premiumLimits: Map<String, Map<String, Int>> = emptyMap(),
    val plans: List<PlanData> = emptyList()
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
            body = "We use your information to create your profile, recommend matches, show compatibility signals, run search filters, verify members, prevent misuse, and provide support."
        ),
        LegalSectionData(
            heading = "Who can see your profile",
            body = "Your visibility and photo privacy settings decide who can see profile details and photos. You can hide or block members when you do not want further interaction."
        ),
        LegalSectionData(
            heading = "Chats and safety",
            body = "Messages, interests, reports, and safety actions may be processed to protect members, investigate complaints, and improve trust on the platform."
        ),
        LegalSectionData(
            heading = "Payments",
            body = "Payment details are handled by authorized payment partners. SoulMatch stores payment status, plan details, invoice data, and transaction references needed for service and support."
        ),
        LegalSectionData(
            heading = "Sharing with partners",
            body = "We share limited information with trusted service providers such as hosting, analytics, payment, notification, and support partners only for operating SoulMatch."
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
    @SerializedName("plan_id") val planId: String = ""
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

fun SearchProfileItem.toProfileSummary(seed: ProfileSummary? = null): ProfileSummary {
    val resolvedName = listOf(safeString(firstName), safeString(lastName)).filter { it.isNotBlank() }.joinToString(" ").ifBlank { seed?.name ?: "Member" }
    return ProfileSummary(
        profileId = profileId,
        userId = if (safeString(userId).isNotBlank()) safeString(userId) else seed?.userId.orEmpty(),
        name = resolvedName,
        age = if (age > 0) age else seed?.age ?: 0,
        location = if (safeString(workingCity).isNotBlank()) safeString(workingCity) else seed?.location.orEmpty(),
        occupation = if (safeString(occupation).isNotBlank()) safeString(occupation) else seed?.occupation.orEmpty(),
        primaryPhoto = primaryPhotoUrl ?: seed?.primaryPhoto,
        compatibilityScore = seed?.compatibilityScore ?: 72,
        compatibilityBreakdown = seed?.compatibilityBreakdown,
        heightCm = heightCm ?: seed?.heightCm,
        isVerified = seed?.isVerified ?: false,
        isPhotoPrivate = seed?.isPhotoPrivate ?: false,
        education = if (safeString(educationLevel).isNotBlank()) safeString(educationLevel) else seed?.education.orEmpty(),
        community = if (safeString(religion).isNotBlank()) safeString(religion) else seed?.community.orEmpty(),
        lastActiveLabel = seed?.lastActiveLabel ?: "Recently active",
        matchReasons = seed?.matchReasons ?: emptyList(),
        interestSent = seed?.interestSent ?: false,
        shortlisted = seed?.shortlisted ?: false,
        profileCreatedBy = if (safeString(profileCreatedBy).isNotBlank()) safeString(profileCreatedBy) else seed?.profileCreatedBy ?: "self"
    )
}
