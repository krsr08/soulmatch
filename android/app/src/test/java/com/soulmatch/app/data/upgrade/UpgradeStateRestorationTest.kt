package com.soulmatch.app.data.upgrade

import org.junit.Assert.assertEquals
import org.junit.Test

class UpgradeStateRestorationTest {
    @Test
    fun refreshStateRestoresTheSameLogicalTabAfterReload() {
        val enabledTabs = UpgradeTabConfig.enabledTabs(UpgradeFeatureFlags(enableOneMonth = true))

        val restored = UpgradeRouteMapping.resolveLandingTabKey(
            pageToLandAfterRefresh = UpgradeTabKey.PERSONALIZED.wireValue,
            enabledTabs = enabledTabs,
            preferRefreshState = true
        )

        assertEquals(UpgradeTabKey.PERSONALIZED, restored)
        assertEquals(4, UpgradeTabConfig.indexOf(restored, enabledTabs))
    }

    @Test
    fun refreshStateDoesNotDependOnTheOneMonthOffset() {
        val enabledTabs = UpgradeTabConfig.enabledTabs(UpgradeFeatureFlags(enableOneMonth = false))

        val restored = UpgradeRouteMapping.resolveLandingTabKey(
            pageToLandAfterRefresh = UpgradeTabKey.PERSONALIZED.wireValue,
            enabledTabs = enabledTabs,
            preferRefreshState = true
        )

        assertEquals(UpgradeTabKey.PERSONALIZED, restored)
        assertEquals(3, UpgradeTabConfig.indexOf(restored, enabledTabs))
    }

    @Test
    fun disabledRestoredTabFallsBackToFirstAvailableTab() {
        val enabledTabs = UpgradeTabConfig.enabledTabs(UpgradeFeatureFlags(enableOneMonth = false))

        val restored = UpgradeRouteMapping.resolveLandingTabKey(
            pageToLandAfterRefresh = UpgradeTabKey.ONE_MONTH.wireValue,
            enabledTabs = enabledTabs,
            preferRefreshState = true
        )

        assertEquals(UpgradeTabKey.THREE_MONTHS, restored)
        assertEquals(0, UpgradeTabConfig.indexOf(restored, enabledTabs))
    }

    @Test
    fun freshDeepLinkCanOverrideStaleRefreshState() {
        val enabledTabs = UpgradeTabConfig.enabledTabs(UpgradeFeatureFlags(enableOneMonth = true))

        val selected = UpgradeRouteMapping.resolveLandingTabKey(
            landOnPage = 6,
            pageToLandAfterRefresh = UpgradeTabKey.PERSONALIZED.wireValue,
            enabledTabs = enabledTabs,
            preferRefreshState = false
        )

        assertEquals(UpgradeTabKey.SIX_MONTHS, selected)
    }

    @Test
    fun packageTargetCanOpenItsOwningTabAfterRefreshDataReloads() {
        val enabledTabs = UpgradeTabConfig.enabledTabs(UpgradeFeatureFlags(enableOneMonth = true))

        val selected = UpgradeRouteMapping.resolveLandingTabKey(
            targetPackageId = "267",
            enabledTabs = enabledTabs
        )

        assertEquals(UpgradeTabKey.TWIN_PACK, selected)
        assertEquals(6, UpgradeTabConfig.indexOf(selected, enabledTabs))
    }
}
