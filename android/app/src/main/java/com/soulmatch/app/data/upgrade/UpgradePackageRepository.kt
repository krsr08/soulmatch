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
            val packageInfo = packagesByPlan[planId] ?: return@mapNotNull null
            val canonical = canonicalPackage(planId, packageInfo)
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

    private fun canonicalPackage(planId: String, source: UpgradePackage? = null): UpgradePackage {
        return when (planId.lowercase()) {
            "gold" -> UpgradePackage(
                pkgId = 201,
                remotePlanId = "gold",
                pkgName = "Gold",
                pkgActualRate = source?.pkgActualRate?.takeIf { it > 0 } ?: 599,
                pkgDiscountedRate = source?.pkgDiscountedRate?.takeIf { it > 0 } ?: 599,
                pkgRate = source?.payableAmount?.takeIf { it > 0 } ?: 599,
                pkgDuration = "Monthly",
                pkgDurationDays = 30,
                pkgPhoneCount = 30,
                pkgBenefit = "For families who want Match Assistance, stronger limits, and 2 spotlight boosts each month.",
                buyerChoice = true,
                badge = "Recommended",
                features = listOf("80 visible matches", "50 profile views", "30 contact unlocks", "Match Assistance", "2 spotlight boosts"),
                assistiveContent = "Best for serious matching with guided help and better reach."
            )
            "platinum" -> UpgradePackage(
                pkgId = 301,
                remotePlanId = "platinum",
                pkgName = "Platinum",
                pkgActualRate = source?.pkgActualRate?.takeIf { it > 0 } ?: 999,
                pkgDiscountedRate = source?.pkgDiscountedRate?.takeIf { it > 0 } ?: 999,
                pkgRate = source?.payableAmount?.takeIf { it > 0 } ?: 999,
                pkgDuration = "Monthly",
                pkgDurationDays = 30,
                pkgPhoneCount = 80,
                pkgBenefit = "For members who need the highest monthly access, Match Assistance, and 4 spotlight boosts.",
                buyerChoice = false,
                badge = "Full access",
                features = listOf("80 visible matches", "80 profile views", "80 contact unlocks", "Match Assistance", "4 spotlight boosts"),
                assistiveContent = "Best when the family wants maximum access and visibility."
            )
            else -> UpgradePackage(
                pkgId = 101,
                remotePlanId = "silver",
                pkgName = "Silver",
                pkgActualRate = source?.pkgActualRate?.takeIf { it > 0 } ?: 299,
                pkgDiscountedRate = source?.pkgDiscountedRate?.takeIf { it > 0 } ?: 299,
                pkgRate = source?.payableAmount?.takeIf { it > 0 } ?: 299,
                pkgDuration = "Monthly",
                pkgDurationDays = 30,
                pkgPhoneCount = 15,
                pkgBenefit = "For members who want chat, Engage+, more shortlists, and 15 contact unlocks each month.",
                buyerChoice = true,
                badge = "Starter",
                features = listOf("80 visible matches", "30 profile views", "15 contact unlocks", "Chat enabled"),
                assistiveContent = "Best first upgrade from Bronze when you are ready to connect."
            )
        }
    }
}
