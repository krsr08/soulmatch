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
        val PROFILE_ID = stringPreferencesKey("profile_id")
        val PLAN_ID = stringPreferencesKey("plan_id")
        val PAGE_TO_LAND_AFTER_REFRESH = stringPreferencesKey("page_to_land_after_refresh")
        val WIZARD_STEP = intPreferencesKey("wizard_step")
        val PUSH_NOTIFICATIONS = booleanPreferencesKey("push_notifications")
        val CONTACT_FILTERS = booleanPreferencesKey("contact_filters")
        val PHOTO_PRIVACY = stringPreferencesKey("photo_privacy")
        val PROFILE_VISIBILITY = stringPreferencesKey("profile_visibility")
    }
    val authToken: Flow<String?> = store.data.map { it[AUTH_TOKEN] }
    val refreshToken: Flow<String?> = store.data.map { it[REFRESH_TOKEN] }
    val userId: Flow<String?> = store.data.map { it[USER_ID] }
    val profileId: Flow<String?> = store.data.map { it[PROFILE_ID] }
    val planId: Flow<String?> = store.data.map { it[PLAN_ID] ?: "free" }
    val pageToLandAfterRefresh: Flow<String?> = store.data.map { it[PAGE_TO_LAND_AFTER_REFRESH] }
    val wizardStep: Flow<Int> = store.data.map { it[WIZARD_STEP] ?: 1 }
    val pushNotifications: Flow<Boolean> = store.data.map { it[PUSH_NOTIFICATIONS] ?: true }
    val contactFilters: Flow<Boolean> = store.data.map { it[CONTACT_FILTERS] ?: false }
    val photoPrivacy: Flow<String> = store.data.map { it[PHOTO_PRIVACY] ?: "matches_only" }
    val profileVisibility: Flow<String> = store.data.map { it[PROFILE_VISIBILITY] ?: "all" }
    suspend fun saveAuthToken(t: String) { store.edit { it[AUTH_TOKEN] = t } }
    suspend fun saveRefreshToken(t: String) { store.edit { it[REFRESH_TOKEN] = t } }
    suspend fun saveUserId(id: String) { store.edit { it[USER_ID] = id } }
    suspend fun saveProfileId(id: String) { store.edit { it[PROFILE_ID] = id } }
    suspend fun savePlanId(p: String) { store.edit { it[PLAN_ID] = p } }
    suspend fun savePageToLandAfterRefresh(tabKey: String) { store.edit { it[PAGE_TO_LAND_AFTER_REFRESH] = tabKey } }
    suspend fun saveWizardStep(s: Int) { store.edit { it[WIZARD_STEP] = s } }
    suspend fun savePushNotifications(enabled: Boolean) { store.edit { it[PUSH_NOTIFICATIONS] = enabled } }
    suspend fun saveContactFilters(enabled: Boolean) { store.edit { it[CONTACT_FILTERS] = enabled } }
    suspend fun savePhotoPrivacy(value: String) { store.edit { it[PHOTO_PRIVACY] = value } }
    suspend fun saveProfileVisibility(value: String) { store.edit { it[PROFILE_VISIBILITY] = value } }
    suspend fun clearProfileProgress() {
        store.edit {
            it.remove(PROFILE_ID)
            it.remove(WIZARD_STEP)
        }
    }
    suspend fun clearAll() { store.edit { it.clear() } }
}
