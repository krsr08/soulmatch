package com.soulmatch.app.data.upgrade

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.soulmatch.app.data.api.PaymentApiService
import com.soulmatch.app.data.config.AppEnvironment
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
        val source = remote ?: if (AppEnvironment.allowDemoFallback) getMockPackageGroups() else emptyList()
        return source.toCanonicalSubscriptionGroups().ifEmpty { defaultSubscriptionGroups() }
    }

    fun getMockPackageGroups(): List<UpgradePackageGroup> {
        val type = object : TypeToken<List<UpgradePackageGroup>>() {}.type
        return context.assets.open(MOCK_FILE).bufferedReader().use { reader ->
            gson.fromJson(reader, type)
        }
    }

    private companion object {
        const val MOCK_FILE = "mock_upgrade_packages.json"
        val CANONICAL_PLAN_ORDER = listOf("silver", "gold", "platinum")
    }

    private fun List<UpgradePackageGroup>.toCanonicalSubscriptionGroups(): List<UpgradePackageGroup> {
        val packagesByPlan = flatMap { it.packages }
            .filter { it.planId.lowercase() in CANONICAL_PLAN_ORDER }
            .groupBy { it.planId.lowercase() }
            .mapValues { (_, packages) ->
                packages.firstOrNull { it.buyerChoice || it.badge.equals("Recommended", ignoreCase = true) }
                    ?: packages.first()
            }

        return CANONICAL_PLAN_ORDER.mapNotNull { planId ->
            if (!packagesByPlan.containsKey(planId)) return@mapNotNull null
            val canonical = canonicalPackage(planId)
            UpgradePackageGroup(
                tabKey = planId,
                tabTitle = canonical.pkgName,
                bannerTitle = "${canonical.pkgName} Membership",
                bannerText = canonical.pkgBenefit,
                assistiveContent = canonical.assistiveContent,
                packages = listOf(canonical)
            )
        }
    }

    private fun defaultSubscriptionGroups(): List<UpgradePackageGroup> {
        return CANONICAL_PLAN_ORDER.map { planId ->
            val packageInfo = canonicalPackage(planId)
            UpgradePackageGroup(
                tabKey = planId,
                tabTitle = packageInfo.pkgName,
                bannerTitle = "${packageInfo.pkgName} Membership",
                bannerText = packageInfo.pkgBenefit,
                assistiveContent = packageInfo.assistiveContent,
                packages = listOf(packageInfo)
            )
        }
    }

    private fun canonicalPackage(planId: String): UpgradePackage {
        return when (planId.lowercase()) {
            "gold" -> UpgradePackage(
                pkgId = 201,
                remotePlanId = "gold",
                pkgName = "Pro Max",
                pkgActualRate = 399,
                pkgDiscountedRate = 399,
                pkgRate = 399,
                pkgDuration = "Monthly",
                pkgDurationDays = 30,
                pkgPhoneCount = 50,
                pkgBenefit = "For families who want stronger limits, 50 Super Interests, one spotlight, and a gold badge.",
                buyerChoice = true,
                badge = "Recommended",
                features = listOf("Contact sharing", "Engage+", "50 contact details", "50 Super Interests", "1 spotlight", "Gold badge"),
                assistiveContent = "Best for serious matching with stronger monthly reach."
            )
            "platinum" -> UpgradePackage(
                pkgId = 301,
                remotePlanId = "platinum",
                pkgName = "Pro Supreme",
                pkgActualRate = 599,
                pkgDiscountedRate = 599,
                pkgRate = 599,
                pkgDuration = "Monthly",
                pkgDurationDays = 30,
                pkgPhoneCount = 80,
                pkgBenefit = "For members who need the highest monthly access, 80 Super Interests, and three spotlights.",
                buyerChoice = false,
                badge = "Top Seller",
                features = listOf("Contact sharing", "Engage+", "80 contact details", "80 Super Interests", "3 spotlights", "Gold badge"),
                assistiveContent = "Best when the family wants maximum access and visibility."
            )
            else -> UpgradePackage(
                pkgId = 101,
                remotePlanId = "silver",
                pkgName = "Pro",
                pkgActualRate = 199,
                pkgDiscountedRate = 199,
                pkgRate = 199,
                pkgDuration = "Monthly",
                pkgDurationDays = 30,
                pkgPhoneCount = 25,
                pkgBenefit = "For members who want contact sharing, Engage+, 25 contact details, and a gold badge.",
                buyerChoice = true,
                badge = "Starter",
                features = listOf("Contact sharing", "Engage+", "25 contact details", "Gold badge"),
                assistiveContent = "Best first upgrade from Bronze when you are ready to connect."
            )
        }
    }
}
