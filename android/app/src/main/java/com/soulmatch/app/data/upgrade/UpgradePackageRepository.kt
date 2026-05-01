package com.soulmatch.app.data.upgrade

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.soulmatch.app.data.api.PaymentApiService
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UpgradePackageRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val paymentApi: PaymentApiService
) {
    private val gson = Gson()

    suspend fun getPackageGroups(): List<UpgradePackageGroup> {
        val remote = runCatching { paymentApi.getUpgradePackageGroups() }
            .getOrNull()
            ?.body()
            ?.takeIf { it.success }
            ?.data
            ?.takeIf { it.isNotEmpty() }
        return remote ?: getMockPackageGroups()
    }

    fun getMockPackageGroups(): List<UpgradePackageGroup> {
        val type = object : TypeToken<List<UpgradePackageGroup>>() {}.type
        return context.assets.open(MOCK_FILE).bufferedReader().use { reader ->
            gson.fromJson(reader, type)
        }
    }

    private companion object {
        const val MOCK_FILE = "mock_upgrade_packages.json"
    }
}
