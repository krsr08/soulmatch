package com.soulmatch.app.data.local
import android.content.Context
import android.content.SharedPreferences
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "soulmatch_prefs")
@Singleton
class UserPreferences @Inject constructor(@ApplicationContext private val context: Context) {
    private val store = context.dataStore
    private val securePrefs: SharedPreferences by lazy {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        EncryptedSharedPreferences.create(
            context,
            "soulmatch_secure_prefs",
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }
    private val authTokenState = MutableStateFlow(securePrefs.getString(SECURE_AUTH_TOKEN, null))
    private val refreshTokenState = MutableStateFlow(securePrefs.getString(SECURE_REFRESH_TOKEN, null))

    companion object {
        private const val SECURE_AUTH_TOKEN = "auth_token"
        private const val SECURE_REFRESH_TOKEN = "refresh_token"
        private const val SECURE_INSTALLATION_ID = "installation_id"
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
    val authToken: Flow<String?> = authTokenState
    val refreshToken: Flow<String?> = refreshTokenState
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
    fun currentAuthToken(): String? = authTokenState.value
    fun currentRefreshToken(): String? = refreshTokenState.value
    fun installationId(): String {
        val existing = securePrefs.getString(SECURE_INSTALLATION_ID, null)
        if (!existing.isNullOrBlank()) return existing
        val created = UUID.randomUUID().toString()
        securePrefs.edit().putString(SECURE_INSTALLATION_ID, created).apply()
        return created
    }
    suspend fun saveAuthToken(t: String) {
        securePrefs.edit().putString(SECURE_AUTH_TOKEN, t).apply()
        authTokenState.value = t
    }
    suspend fun saveRefreshToken(t: String) {
        securePrefs.edit().putString(SECURE_REFRESH_TOKEN, t).apply()
        refreshTokenState.value = t
    }
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
    suspend fun clearAll() {
        securePrefs.edit().clear().apply()
        authTokenState.value = null
        refreshTokenState.value = null
        store.edit { it.clear() }
    }
}
