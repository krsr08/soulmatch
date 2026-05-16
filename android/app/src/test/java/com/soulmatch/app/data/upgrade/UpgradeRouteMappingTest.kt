package com.soulmatch.app.data.upgrade

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class UpgradeRouteMappingTest {
    @Test
    fun routeCodesResolveToSoulMatchSubscriptionPacks() {
        assertEquals(UpgradeTabKey.SILVER, UpgradeRouteMapping.tabKeyForRouteCode(0))
        assertEquals(UpgradeTabKey.GOLD, UpgradeRouteMapping.tabKeyForRouteCode(1))
        assertEquals(UpgradeTabKey.PLATINUM, UpgradeRouteMapping.tabKeyForRouteCode(2))
        assertEquals(UpgradeTabKey.PLATINUM, UpgradeRouteMapping.tabKeyForRouteCode(3))
        assertEquals(UpgradeTabKey.GOLD, UpgradeRouteMapping.tabKeyForRouteCode(33))
        assertNull(UpgradeRouteMapping.tabKeyForRouteCode(99))
    }

    @Test
    fun packageIdsResolveToCanonicalSubscriptionPacks() {
        assertEquals(UpgradeTabKey.SILVER, UpgradeRouteMapping.tabKeyForPackageId("101"))
        assertEquals(UpgradeTabKey.GOLD, UpgradeRouteMapping.tabKeyForPackageId("201"))
        assertEquals(UpgradeTabKey.PLATINUM, UpgradeRouteMapping.tabKeyForPackageId("301"))
        assertEquals(UpgradeTabKey.SILVER, UpgradeRouteMapping.tabKeyForPackageId("322"))
        assertEquals(UpgradeTabKey.GOLD, UpgradeRouteMapping.tabKeyForPackageId("13"))
        assertEquals(UpgradeTabKey.PLATINUM, UpgradeRouteMapping.tabKeyForPackageId("288"))
    }

    @Test
    fun tabOrderUsesOnlyAppSubscriptionPacks() {
        val tabs = UpgradeTabConfig.enabledTabs()

        assertEquals(listOf(UpgradeTabKey.SILVER, UpgradeTabKey.GOLD, UpgradeTabKey.PLATINUM), tabs)
        assertEquals(0, UpgradeTabConfig.indexOf(UpgradeTabKey.SILVER, tabs))
        assertEquals(1, UpgradeTabConfig.indexOf(UpgradeTabKey.GOLD, tabs))
        assertEquals(2, UpgradeTabConfig.indexOf(UpgradeTabKey.PLATINUM, tabs))
        assertEquals(-1, UpgradeTabConfig.indexOf(UpgradeTabKey.ONE_MONTH, tabs))
    }

    @Test
    fun incomingCodesResolveBeforeIndexesAreCalculated() {
        val enabledTabs = UpgradeTabConfig.enabledTabs()

        val tab = UpgradeRouteMapping.resolveLandingTabKey(
            landOnPage = 14,
            enabledTabs = enabledTabs
        )

        assertEquals(UpgradeTabKey.GOLD, tab)
        assertEquals(1, UpgradeTabConfig.indexOf(tab, enabledTabs))
    }
}
