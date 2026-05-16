package com.soulmatch.app.data.upgrade

import org.junit.Assert.assertEquals
import org.junit.Test

class UpgradeStateRestorationTest {
    @Test
    fun refreshStateRestoresTheSameSubscriptionPackAfterReload() {
        val enabledTabs = UpgradeTabConfig.enabledTabs()

        val restored = UpgradeRouteMapping.resolveLandingTabKey(
            pageToLandAfterRefresh = UpgradeTabKey.PLATINUM.wireValue,
            enabledTabs = enabledTabs,
            preferRefreshState = true
        )

        assertEquals(UpgradeTabKey.PLATINUM, restored)
        assertEquals(2, UpgradeTabConfig.indexOf(restored, enabledTabs))
    }

    @Test
    fun disabledLegacyRestoredTabFallsBackToFirstSubscriptionPack() {
        val enabledTabs = UpgradeTabConfig.enabledTabs()

        val restored = UpgradeRouteMapping.resolveLandingTabKey(
            pageToLandAfterRefresh = UpgradeTabKey.ONE_MONTH.wireValue,
            enabledTabs = enabledTabs,
            preferRefreshState = true
        )

        assertEquals(UpgradeTabKey.SILVER, restored)
        assertEquals(0, UpgradeTabConfig.indexOf(restored, enabledTabs))
    }

    @Test
    fun freshDeepLinkCanOverrideStaleRefreshState() {
        val enabledTabs = UpgradeTabConfig.enabledTabs()

        val selected = UpgradeRouteMapping.resolveLandingTabKey(
            landOnPage = 6,
            pageToLandAfterRefresh = UpgradeTabKey.PLATINUM.wireValue,
            enabledTabs = enabledTabs,
            preferRefreshState = false
        )

        assertEquals(UpgradeTabKey.GOLD, selected)
    }

    @Test
    fun packageTargetCanOpenItsOwningSubscriptionPackAfterRefreshDataReloads() {
        val enabledTabs = UpgradeTabConfig.enabledTabs()

        val selected = UpgradeRouteMapping.resolveLandingTabKey(
            targetPackageId = "301",
            enabledTabs = enabledTabs
        )

        assertEquals(UpgradeTabKey.PLATINUM, selected)
        assertEquals(2, UpgradeTabConfig.indexOf(selected, enabledTabs))
    }
}
