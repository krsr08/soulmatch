package com.soulmatch.app.data.upgrade

object UpgradeRouteMapping {
    private val routeCodeToTabKey = mapOf(
        0 to UpgradeTabKey.THREE_MONTHS,
        1 to UpgradeTabKey.SIX_MONTHS,
        2 to UpgradeTabKey.TILL_U_MARRY,
        3 to UpgradeTabKey.PERSONALIZED,
        5 to UpgradeTabKey.THREE_MONTHS,
        6 to UpgradeTabKey.SIX_MONTHS,
        8 to UpgradeTabKey.PERSONALIZED,
        11 to UpgradeTabKey.THREE_MONTHS,
        12 to UpgradeTabKey.THREE_MONTHS,
        13 to UpgradeTabKey.THREE_MONTHS,
        14 to UpgradeTabKey.SIX_MONTHS,
        15 to UpgradeTabKey.SIX_MONTHS,
        16 to UpgradeTabKey.SIX_MONTHS,
        17 to UpgradeTabKey.TILL_U_MARRY,
        18 to UpgradeTabKey.PERSONALIZED,
        21 to UpgradeTabKey.TILL_U_MARRY,
        33 to UpgradeTabKey.THREE_MONTHS
    )

    private val packageIdToTabKey = buildMap {
        listOf(322, 323, 324, 325).forEach { put(it, UpgradeTabKey.ONE_MONTH) }
        listOf(1, 4, 13, 33).forEach { put(it, UpgradeTabKey.THREE_MONTHS) }
        listOf(2, 5, 14, 16).forEach { put(it, UpgradeTabKey.SIX_MONTHS) }
        listOf(237, 238, 239, 273, 275).forEach { put(it, UpgradeTabKey.TILL_U_MARRY) }
        listOf(48, 80, 306).forEach { put(it, UpgradeTabKey.PERSONALIZED) }
        listOf(241).forEach { put(it, UpgradeTabKey.ELITE) }
        listOf(266, 267, 288).forEach { put(it, UpgradeTabKey.TWIN_PACK) }
    }

    fun tabKeyForRouteCode(routeCode: Int?): UpgradeTabKey? {
        return routeCode?.let(routeCodeToTabKey::get)
    }

    fun tabKeyForPackageId(targetPackageId: String?): UpgradeTabKey? {
        val id = targetPackageId?.trim()?.toIntOrNull() ?: return null
        return packageIdToTabKey[id]
    }

    fun resolveLandingTabKey(
        landOnPage: Int? = null,
        routeCode: Int? = null,
        targetPackageId: String? = null,
        pageToLandAfterRefresh: String? = null,
        enabledTabs: List<UpgradeTabKey>,
        preferRefreshState: Boolean = false
    ): UpgradeTabKey {
        val refreshTab = UpgradeTabKey.from(pageToLandAfterRefresh)
        val targetPackageTab = tabKeyForPackageId(targetPackageId)
        val routeTab = tabKeyForRouteCode(routeCode ?: landOnPage)
        val defaultTab = UpgradeTabKey.THREE_MONTHS

        val candidates = if (preferRefreshState) {
            listOf(refreshTab, targetPackageTab, routeTab, defaultTab)
        } else {
            listOf(targetPackageTab, routeTab, refreshTab, defaultTab)
        }
        return candidates
            .filterNotNull()
            .firstOrNull { it in enabledTabs }
            ?: UpgradeTabConfig.firstAvailable(enabledTabs)
    }

    fun tabKeyForSelectedPackage(pkgId: Int?): UpgradeTabKey? {
        return pkgId?.let(packageIdToTabKey::get)
    }
}
