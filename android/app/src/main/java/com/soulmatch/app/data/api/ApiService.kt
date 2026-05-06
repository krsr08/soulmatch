package com.soulmatch.app.data.api
import com.soulmatch.app.data.models.*
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Response
import retrofit2.http.DELETE
import retrofit2.http.*
import kotlin.jvm.JvmSuppressWildcards
interface AuthApiService {
    @POST("auth/send-otp") suspend fun sendOTP(@Body req: SendOTPRequest): Response<GenericResponse<Any>>
    @POST("auth/verify-otp") suspend fun verifyOTP(@Body req: VerifyOTPRequest): Response<GenericResponse<AuthData>>
    @POST("auth/google-login") suspend fun googleLogin(@Body req: GoogleLoginRequest): Response<GenericResponse<AuthData>>
    @POST("auth/firebase-phone-login") suspend fun firebasePhoneLogin(@Body req: FirebasePhoneLoginRequest): Response<GenericResponse<AuthData>>
    @POST("auth/select-user-type") suspend fun selectUserType(@Body req: SelectUserTypeRequest): Response<GenericResponse<AuthData>>
    @POST("auth/refresh-token") suspend fun refreshToken(@Body req: RefreshTokenRequest): Response<GenericResponse<AuthData>>
    @POST("auth/logout") suspend fun logout(@Body req: Map<String, @JvmSuppressWildcards String>): Response<GenericResponse<Any>>
}
interface ProfileApiService {
    @POST("profile/create") suspend fun createProfileStep(@Body req: Map<String, @JvmSuppressWildcards Any>): Response<GenericResponse<ProfileStepResponse>>
    @GET("profile/me") suspend fun getMyProfile(): Response<GenericResponse<ProfileData>>
    @GET("profile/agent/me") suspend fun getAgentProfile(): Response<GenericResponse<AgentProfileData>>
    @PUT("profile/agent/me") suspend fun upsertAgentProfile(@Body req: AgentProfileUpsertRequest): Response<GenericResponse<AgentProfileData>>
    @POST("profile/agent/onboarding") suspend fun submitAgentOnboarding(@Body req: AgentOnboardingRequest): Response<GenericResponse<AgentProfileData>>
    @Multipart
    @POST("profile/agent/onboarding")
    suspend fun submitAgentOnboardingMultipart(
        @PartMap fields: Map<String, @JvmSuppressWildcards RequestBody>,
        @Part documents: List<MultipartBody.Part>
    ): Response<GenericResponse<AgentProfileData>>
    @GET("profile/agent/membership") suspend fun getAgentMembership(): Response<GenericResponse<AgentMembershipData>>
    @GET("profile/agent/membership/plans") suspend fun getAgentMembershipPlans(): Response<GenericResponse<List<AgentMembershipData>>>
    @GET("profile/agent/managed-profiles") suspend fun getAgentManagedProfiles(): Response<GenericResponse<List<AgentManagedProfileSummaryData>>>
    @GET("profile/assist/status") suspend fun getAssistStatus(): Response<GenericResponse<AssistStatusData>>
    @PUT("profile/assist/status") suspend fun updateAssistStatus(@Body req: AssistStatusRequest): Response<GenericResponse<AssistStatusData>>
    @GET("profile/family-decisions") suspend fun getFamilyDecisions(): Response<GenericResponse<List<FamilyDecisionData>>>
    @PUT("profile/family-decisions/{targetProfileId}") suspend fun upsertFamilyDecision(@Path("targetProfileId") id: String, @Body req: FamilyDecisionRequest): Response<GenericResponse<FamilyDecisionData>>
    @POST("profile/family-decisions/{familyDecisionId}/comments") suspend fun addFamilyDecisionComment(@Path("familyDecisionId") id: String, @Body req: FamilyDecisionCommentRequest): Response<GenericResponse<FamilyDecisionCommentData>>
    @GET("profile/{profileId}") suspend fun getProfile(@Path("profileId") id: String): Response<GenericResponse<ProfileData>>
    @GET("profile/{profileId}/photos") suspend fun getPhotos(@Path("profileId") id: String): Response<GenericResponse<List<ProfilePhoto>>>
    @Multipart
    @POST("profile/{profileId}/photos")
    suspend fun uploadPhotos(@Path("profileId") id: String, @Part photos: List<MultipartBody.Part>): Response<GenericResponse<PhotoUploadData>>
    @DELETE("profile/{profileId}/photos/{photoId}") suspend fun deletePhoto(@Path("profileId") profileId: String, @Path("photoId") photoId: String): Response<GenericResponse<Any>>
    @PUT("profile/{profileId}/photos/{photoId}/primary") suspend fun setPrimaryPhoto(@Path("profileId") profileId: String, @Path("photoId") photoId: String): Response<GenericResponse<Any>>
    @GET("profile/{profileId}/preferences") suspend fun getPreferences(@Path("profileId") id: String): Response<GenericResponse<PartnerPreferencesData>>
    @PUT("profile/{profileId}/preferences") suspend fun updatePreferences(@Path("profileId") id: String, @Body req: PartnerPreferencesData): Response<GenericResponse<Any>>
    @POST("profile/{profileId}/match-feedback") suspend fun recordMatchFeedback(@Path("profileId") id: String, @Body req: MatchFeedbackRequest): Response<GenericResponse<Any>>
    @PUT("profile/{profileId}/privacy") suspend fun updatePrivacy(@Path("profileId") id: String, @Body req: PrivacySettingsRequest): Response<GenericResponse<Any>>
    @PATCH("profile/status") suspend fun updateProfileStatus(@Body req: ProfileStatusRequest): Response<GenericResponse<Any>>
    @POST("profile/{profileId}/photo-access/request") suspend fun requestPhotoAccess(@Path("profileId") id: String, @Body req: PhotoAccessRequestBody = PhotoAccessRequestBody()): Response<GenericResponse<PhotoAccessResponseData>>
    @GET("profile/photo-access/requests") suspend fun getPhotoAccessRequests(): Response<GenericResponse<List<PhotoAccessRequestData>>>
    @PUT("profile/photo-access/requests/{requestId}") suspend fun respondPhotoAccessRequest(@Path("requestId") id: String, @Body req: PhotoAccessActionRequest): Response<GenericResponse<PhotoAccessResponseData>>
    @GET("profile/{profileId}/verifications") suspend fun getVerifications(@Path("profileId") id: String): Response<GenericResponse<List<VerificationRequestData>>>
    @POST("profile/{profileId}/verifications") suspend fun submitVerification(@Path("profileId") id: String, @Body req: VerificationSubmitRequest): Response<GenericResponse<VerificationRequestData>>
    @GET("profile/{profileId}/viewers") suspend fun getViewers(@Path("profileId") id: String): Response<GenericResponse<List<ViewerData>>>
    @POST("profile/{profileId}/block") suspend fun blockProfile(@Path("profileId") id: String): Response<GenericResponse<Any>>
    @POST("profile/{profileId}/report") suspend fun reportProfile(@Path("profileId") id: String, @Body req: Map<String, @JvmSuppressWildcards String>): Response<GenericResponse<Any>>
}
interface MatchingApiService {
    @GET("matches/recommended") suspend fun getRecommended(@Query("page") page: Int = 1, @Query("limit") limit: Int = 25, @Query("verifiedOnly") verifiedOnly: Boolean = false): Response<GenericResponse<MatchesData>>
    @GET("matches/compatibility/{profileId}") suspend fun getCompatibility(@Path("profileId") id: String): Response<GenericResponse<CompatibilityData>>
}
interface SearchApiService {
    @POST("search/basic") suspend fun basicSearch(@Body req: SearchRequest): Response<GenericResponse<SearchResultsData>>
    @POST("search/advanced") suspend fun advancedSearch(@Body req: SearchRequest): Response<GenericResponse<SearchResultsData>>
    @GET("search/saved") suspend fun getSavedSearches(): Response<GenericResponse<List<SavedSearchData>>>
    @POST("search/save") suspend fun saveSearch(@Body req: SearchRequest): Response<GenericResponse<SavedSearchData>>
}
interface InterestApiService {
    @POST("interests/send") suspend fun sendInterest(@Body req: InterestRequest): Response<GenericResponse<InterestResult>>
    @GET("interests/received") suspend fun getReceived(): Response<GenericResponse<List<InterestListItem>>>
    @GET("interests/sent") suspend fun getSent(): Response<GenericResponse<List<InterestListItem>>>
    @PUT("interests/{id}/respond") suspend fun respond(@Path("id") id: String, @Body req: RespondRequest): Response<GenericResponse<InterestResult>>
    @POST("interests/shortlist/{profileId}") suspend fun toggleShortlist(@Path("profileId") id: String): Response<GenericResponse<ActionResult>>
    @GET("interests/shortlist") suspend fun getShortlist(): Response<GenericResponse<List<ShortlistItem>>>
}
interface PaymentApiService {
    @GET("payment/plans") suspend fun getPlans(): Response<GenericResponse<List<PlanData>>>
    @GET("payment/upgrade-packages") suspend fun getUpgradePackageGroups(): Response<GenericResponse<List<com.soulmatch.app.data.upgrade.UpgradePackageGroup>>>
    @POST("payment/create-order") suspend fun createOrder(@Body req: OrderRequest): Response<GenericResponse<OrderData>>
    @POST("payment/verify") suspend fun verifyPayment(@Body req: PaymentVerifyRequest): Response<GenericResponse<Any>>
    @GET("payment/subscription") suspend fun getSubscription(): Response<GenericResponse<SubscriptionData>>
    @GET("payment/invoices") suspend fun getInvoices(): Response<GenericResponse<List<InvoiceItem>>>
}
interface ControlPlaneApiService {
    @GET("public/config") suspend fun getRuntimeConfig(): Response<GenericResponse<RuntimeConfigData>>
    @POST("public/analytics") suspend fun trackAnalytics(@Body req: AnalyticsEventRequest): Response<GenericResponse<Any>>
}
interface NotificationApiService {
    @POST("notifications/devices/fcm-token") suspend fun registerFcmToken(@Body req: FcmTokenRequest): Response<GenericResponse<Any>>
    @GET("notifications") suspend fun getNotifications(): Response<GenericResponse<List<NotificationData>>>
    @PUT("notifications/{id}/read") suspend fun markRead(@Path("id") id: String): Response<GenericResponse<Any>>
    @PUT("notifications/mark-all-read") suspend fun markAllRead(): Response<GenericResponse<Any>>
}
interface ChatApiService {
    @GET("chat/conversations") suspend fun getConversations(): Response<GenericResponse<List<ConversationItem>>>
    @GET("chat/{chatId}/messages") suspend fun getMessages(@Path("chatId") chatId: String, @Query("page") page: Int = 1): Response<GenericResponse<MessagePageData>>
    @GET("chat/eligibility/{targetUserId}") suspend fun checkEligibility(@Path("targetUserId") id: String): Response<GenericResponse<ChatEligibilityData>>
    @POST("chat/messages/{messageId}/report") suspend fun reportMessage(@Path("messageId") id: String, @Body req: Map<String, @JvmSuppressWildcards String>): Response<GenericResponse<Any>>
}
