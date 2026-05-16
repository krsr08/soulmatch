package com.soulmatch.app.data.upgrade

object UpgradeRouteMapping {
    private val routeCodeToTabKey = mapOf(
        0 to UpgradeTabKey.SILVER,
        1 to UpgradeTabKey.GOLD,
        2 to UpgradeTabKey.PLATINUM,
        3 to UpgradeTabKey.PLATINUM,
        5 to UpgradeTabKey.SILVER,
        6 to UpgradeTabKey.GOLD,
        8 to UpgradeTabKey.PLATINUM,
        11 to UpgradeTabKey.SILVER,
        12 to UpgradeTabKey.SILVER,
        13 to UpgradeTabKey.GOLD,
        14 to UpgradeTabKey.GOLD,
        15 to UpgradeTabKey.GOLD,
        16 to UpgradeTabKey.GOLD,
        17 to UpgradeTabKey.PLATINUM,
        18 to UpgradeTabKey.PLATINUM,
        21 to UpgradeTabKey.PLATINUM,
        33 to UpgradeTabKey.GOLD
    )

    private val packageIdToTabKey = buildMap {
        put(101, UpgradeTabKey.SILVER)
        put(201, UpgradeTabKey.GOLD)
        put(301, UpgradeTabKey.PLATINUM)
        listOf(322, 323, 324, 325).forEach { put(it, UpgradeTabKey.SILVER) }
        listOf(1, 4, 13, 33, 2, 5, 14, 16).forEach { put(it, UpgradeTabKey.GOLD) }
        listOf(237, 238, 239, 273, 275, 48, 80, 306, 241, 266, 267, 288).forEach { put(it, UpgradeTabKey.PLATINUM) }
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
        val defaultTab = UpgradeTabKey.SILVER

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
