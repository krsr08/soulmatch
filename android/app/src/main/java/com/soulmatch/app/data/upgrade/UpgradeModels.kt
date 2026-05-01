package com.soulmatch.app.data.upgrade

import com.google.gson.annotations.SerializedName

enum class UpgradeTabKey(val wireValue: String, val displayTitle: String) {
    ONE_MONTH("one_month", "1 Month"),
    THREE_MONTHS("three_months", "3 Months"),
    SIX_MONTHS("six_months", "6 Months"),
    TILL_U_MARRY("till_u_marry", "Till U Marry"),
    PERSONALIZED("personalized", "Personalized"),
    ELITE("elite", "Elite"),
    TWIN_PACK("twin_pack", "Twin Pack");

    companion object {
        fun from(value: String?): UpgradeTabKey? {
            if (value.isNullOrBlank()) return null
            return values().firstOrNull { it.wireValue == value || it.name.equals(value, ignoreCase = true) }
        }
    }
}

data class UpgradeFeatureFlags(
    val enableOneMonth: Boolean = true,
    val enableThreeMonths: Boolean = true,
    val enableSixMonths: Boolean = true,
    val enableTillUMarry: Boolean = true,
    val enablePersonalized: Boolean = true,
    val enableElite: Boolean = true,
    val enableTwinPack: Boolean = true
) {
    fun isEnabled(tabKey: UpgradeTabKey): Boolean {
        return when (tabKey) {
            UpgradeTabKey.ONE_MONTH -> enableOneMonth
            UpgradeTabKey.THREE_MONTHS -> enableThreeMonths
            UpgradeTabKey.SIX_MONTHS -> enableSixMonths
            UpgradeTabKey.TILL_U_MARRY -> enableTillUMarry
            UpgradeTabKey.PERSONALIZED -> enablePersonalized
            UpgradeTabKey.ELITE -> enableElite
            UpgradeTabKey.TWIN_PACK -> enableTwinPack
        }
    }
}

data class UpgradePackageGroup(
    @SerializedName("tabKey") val tabKey: String = "",
    @SerializedName("tabTitle") val tabTitle: String = "",
    @SerializedName("bannerTitle") val bannerTitle: String = "",
    @SerializedName("bannerText") val bannerText: String = "",
    @SerializedName("assistiveContent") val assistiveContent: String = "",
    @SerializedName("packages") val packages: List<UpgradePackage> = emptyList()
) {
    val semanticKey: UpgradeTabKey?
        get() = UpgradeTabKey.from(tabKey)
}

data class UpgradePackage(
    @SerializedName("pkgId") val pkgId: Int = 0,
    @SerializedName("pkgName") val pkgName: String = "",
    @SerializedName("pkgActualRate") val pkgActualRate: Int = 0,
    @SerializedName("pkgDiscountedRate") val pkgDiscountedRate: Int = 0,
    @SerializedName("pkgRate") val pkgRate: Int = 0,
    @SerializedName("pkgDuration") val pkgDuration: String = "",
    @SerializedName("pkgDurationDays") val pkgDurationDays: Int = 0,
    @SerializedName("pkgPhoneCount") val pkgPhoneCount: Int = 0,
    @SerializedName("pkgBenefit") val pkgBenefit: String = "",
    @SerializedName("pkgBenefitImg") val pkgBenefitImg: String = "",
    @SerializedName("choiceImage") val choiceImage: String = "",
    @SerializedName("linearContent") val linearContent: String = "",
    @SerializedName("buyerChoice") val buyerChoice: Boolean = false,
    @SerializedName("badge") val badge: String? = null,
    @SerializedName("toiPkgAmount") val toiPkgAmount: Int? = null,
    @SerializedName("toiPublishDate") val toiPublishDate: String? = null,
    @SerializedName("features") val features: List<String> = emptyList(),
    @SerializedName("assistiveContent") val assistiveContent: String = ""
) {
    val planId: String
        get() = pkgId.toString()

    val displayName: String
        get() {
            val durationLabel = when {
                pkgDurationDays in 1..31 -> "1 Month"
                pkgDurationDays in 60..100 -> "3 Months"
                pkgDurationDays in 150..220 -> "6 Months"
                else -> pkgDuration
            }
            return when {
                pkgId == 241 -> "SoulMatch Elite"
                pkgId in setOf(266, 267, 288) -> "SoulMatch Twin Pack $durationLabel"
                pkgId in setOf(48, 80, 306) -> "SoulMatch Personalized $durationLabel"
                pkgId in setOf(237, 238, 239, 273, 275) -> "SoulMatch Till U Marry"
                pkgName.contains("Premium", ignoreCase = true) -> "SoulMatch Premium $durationLabel"
                pkgName.contains("Advantage", ignoreCase = true) -> "SoulMatch Advantage $durationLabel"
                else -> "SoulMatch Classic $durationLabel"
            }
        }

    val payableAmount: Int
        get() = when {
            pkgRate > 0 -> pkgRate
            pkgDiscountedRate > 0 -> pkgDiscountedRate
            else -> pkgActualRate
        }

    val savingsAmount: Int
        get() = (pkgActualRate - payableAmount).coerceAtLeast(0)

    fun perDayAmount(): Int? {
        if (pkgDurationDays <= 0 || payableAmount <= 0) return null
        return (payableAmount + pkgDurationDays - 1) / pkgDurationDays
    }

    fun perMonthAmount(): Int? {
        val months = (pkgDurationDays / 30).coerceAtLeast(1)
        if (payableAmount <= 0) return null
        return (payableAmount + months - 1) / months
    }
}

data class UpgradeLandingArgs(
    val landOnPage: Int? = null,
    val routeCode: Int? = null,
    val targetPackageId: String? = null
) {
    val hasIncomingTarget: Boolean
        get() = landOnPage != null || routeCode != null || !targetPackageId.isNullOrBlank()
}
