package com.soulmatch.app.data.local
import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton
val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "soulmatch_prefs")
@Singleton
class UserPreferences @Inject constructor(@ApplicationContext private val context: Context) {
    private val store = context.dataStore
    companion object {
        val AUTH_TOKEN = stringPreferencesKey("auth_token")
        val REFRESH_TOKEN = stringPreferencesKey("refresh_token")
        val USER_ID = stringPreferencesKey("user_id")
        val USER_TYPE = stringPreferencesKey("user_type")
        val ADVISOR_ID = stringPreferencesKey("advisor_id")
        val PROFILE_ID = stringPreferencesKey("profile_id")
        val PLAN_ID = stringPreferencesKey("plan_id")
        val PAGE_TO_LAND_AFTER_REFRESH = stringPreferencesKey("page_to_land_after_refresh")
        val PENDING_AUTH_ROUTE = stringPreferencesKey("pending_auth_route")
        val WIZARD_STEP = intPreferencesKey("wizard_step")
        val PUSH_NOTIFICATIONS = booleanPreferencesKey("push_notifications")
        val NOTIFICATION_PROMPT_DISMISSED = booleanPreferencesKey("notification_prompt_dismissed")
        val CONTACT_FILTERS = booleanPreferencesKey("contact_filters")
        val PHOTO_PRIVACY = stringPreferencesKey("photo_privacy")
        val PROFILE_VISIBILITY = stringPreferencesKey("profile_visibility")
        val PENDING_OTP_PHONE = stringPreferencesKey("pending_otp_phone")
        val PENDING_OTP_VERIFICATION_ID = stringPreferencesKey("pending_otp_verification_id")
    }
    val authToken: Flow<String?> = store.data.map { it[AUTH_TOKEN] }
    val refreshToken: Flow<String?> = store.data.map { it[REFRESH_TOKEN] }
    val userId: Flow<String?> = store.data.map { it[USER_ID] }
    val userType: Flow<String?> = store.data.map { it[USER_TYPE] }
    val advisorId: Flow<String?> = store.data.map { it[ADVISOR_ID] }
    val profileId: Flow<String?> = store.data.map { it[PROFILE_ID] }
    val planId: Flow<String?> = store.data.map { it[PLAN_ID] ?: "free" }
    val pageToLandAfterRefresh: Flow<String?> = store.data.map { it[PAGE_TO_LAND_AFTER_REFRESH] }
    val pendingAuthRoute: Flow<String?> = store.data.map { it[PENDING_AUTH_ROUTE] }
    val wizardStep: Flow<Int> = store.data.map { it[WIZARD_STEP] ?: 1 }
    val pushNotifications: Flow<Boolean> = store.data.map { it[PUSH_NOTIFICATIONS] ?: true }
    val notificationPromptDismissed: Flow<Boolean> = store.data.map { it[NOTIFICATION_PROMPT_DISMISSED] ?: false }
    val contactFilters: Flow<Boolean> = store.data.map { it[CONTACT_FILTERS] ?: false }
    val photoPrivacy: Flow<String> = store.data.map { it[PHOTO_PRIVACY] ?: "matches_only" }
    val profileVisibility: Flow<String> = store.data.map { it[PROFILE_VISIBILITY] ?: "all" }
    val pendingOtpPhone: Flow<String?> = store.data.map { it[PENDING_OTP_PHONE] }
    val pendingOtpVerificationId: Flow<String?> = store.data.map { it[PENDING_OTP_VERIFICATION_ID] }
    suspend fun saveAuthToken(t: String) { store.edit { it[AUTH_TOKEN] = t } }
    suspend fun saveRefreshToken(t: String) { store.edit { it[REFRESH_TOKEN] = t } }
    suspend fun saveUserId(id: String) { store.edit { it[USER_ID] = id } }
    suspend fun saveUserType(type: String) { store.edit { it[USER_TYPE] = type } }
    suspend fun clearUserType() { store.edit { it.remove(USER_TYPE) } }
    suspend fun saveAdvisorId(id: String) {
        store.edit {
            if (id.isBlank()) it.remove(ADVISOR_ID) else it[ADVISOR_ID] = id
        }
    }
    suspend fun saveProfileId(id: String) { store.edit { it[PROFILE_ID] = id } }
    suspend fun savePlanId(p: String) { store.edit { it[PLAN_ID] = p } }
    suspend fun savePageToLandAfterRefresh(tabKey: String) { store.edit { it[PAGE_TO_LAND_AFTER_REFRESH] = tabKey } }
    suspend fun savePendingAuthRoute(route: String) { store.edit { it[PENDING_AUTH_ROUTE] = route } }
    suspend fun clearPendingAuthRoute() { store.edit { it.remove(PENDING_AUTH_ROUTE) } }
    suspend fun saveWizardStep(s: Int) { store.edit { it[WIZARD_STEP] = s } }
    suspend fun savePushNotifications(enabled: Boolean) { store.edit { it[PUSH_NOTIFICATIONS] = enabled } }
    suspend fun saveNotificationPromptDismissed(dismissed: Boolean) { store.edit { it[NOTIFICATION_PROMPT_DISMISSED] = dismissed } }
    suspend fun saveContactFilters(enabled: Boolean) { store.edit { it[CONTACT_FILTERS] = enabled } }
    suspend fun savePhotoPrivacy(value: String) { store.edit { it[PHOTO_PRIVACY] = value } }
    suspend fun saveProfileVisibility(value: String) { store.edit { it[PROFILE_VISIBILITY] = value } }
    suspend fun savePendingOtpSession(phone: String, verificationId: String) {
        store.edit {
            it[PENDING_OTP_PHONE] = phone
            it[PENDING_OTP_VERIFICATION_ID] = verificationId
        }
    }
    suspend fun clearPendingOtpSession() {
        store.edit {
            it.remove(PENDING_OTP_PHONE)
            it.remove(PENDING_OTP_VERIFICATION_ID)
        }
    }
    suspend fun clearProfileProgress() {
        store.edit {
            it.remove(PROFILE_ID)
            it.remove(WIZARD_STEP)
        }
    }
    suspend fun clearAll() { store.edit { it.clear() } }
}
