package com.soulmatch.app.data.upgrade

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class UpgradeRouteMappingTest {
    @Test
    fun routeCodesResolveToSemanticTabs() {
        assertEquals(UpgradeTabKey.THREE_MONTHS, UpgradeRouteMapping.tabKeyForRouteCode(0))
        assertEquals(UpgradeTabKey.SIX_MONTHS, UpgradeRouteMapping.tabKeyForRouteCode(1))
        assertEquals(UpgradeTabKey.TILL_U_MARRY, UpgradeRouteMapping.tabKeyForRouteCode(2))
        assertEquals(UpgradeTabKey.PERSONALIZED, UpgradeRouteMapping.tabKeyForRouteCode(3))
        assertEquals(UpgradeTabKey.THREE_MONTHS, UpgradeRouteMapping.tabKeyForRouteCode(33))
        assertNull(UpgradeRouteMapping.tabKeyForRouteCode(99))
    }

    @Test
    fun packageIdsResolveToPackageCategories() {
        assertEquals(UpgradeTabKey.ONE_MONTH, UpgradeRouteMapping.tabKeyForPackageId("322"))
        assertEquals(UpgradeTabKey.THREE_MONTHS, UpgradeRouteMapping.tabKeyForPackageId("13"))
        assertEquals(UpgradeTabKey.SIX_MONTHS, UpgradeRouteMapping.tabKeyForPackageId("14"))
        assertEquals(UpgradeTabKey.TILL_U_MARRY, UpgradeRouteMapping.tabKeyForPackageId("275"))
        assertEquals(UpgradeTabKey.PERSONALIZED, UpgradeRouteMapping.tabKeyForPackageId("306"))
        assertEquals(UpgradeTabKey.ELITE, UpgradeRouteMapping.tabKeyForPackageId("241"))
        assertEquals(UpgradeTabKey.TWIN_PACK, UpgradeRouteMapping.tabKeyForPackageId("288"))
    }

    @Test
    fun tabOrderUsesSemanticKeysWhenOneMonthIsEnabled() {
        val tabs = UpgradeTabConfig.enabledTabs(UpgradeFeatureFlags(enableOneMonth = true))

        assertEquals(0, UpgradeTabConfig.indexOf(UpgradeTabKey.ONE_MONTH, tabs))
        assertEquals(1, UpgradeTabConfig.indexOf(UpgradeTabKey.THREE_MONTHS, tabs))
        assertEquals(2, UpgradeTabConfig.indexOf(UpgradeTabKey.SIX_MONTHS, tabs))
        assertEquals(3, UpgradeTabConfig.indexOf(UpgradeTabKey.TILL_U_MARRY, tabs))
        assertEquals(4, UpgradeTabConfig.indexOf(UpgradeTabKey.PERSONALIZED, tabs))
        assertEquals(5, UpgradeTabConfig.indexOf(UpgradeTabKey.ELITE, tabs))
        assertEquals(6, UpgradeTabConfig.indexOf(UpgradeTabKey.TWIN_PACK, tabs))
    }

    @Test
    fun tabOrderUsesSemanticKeysWhenOneMonthIsDisabled() {
        val tabs = UpgradeTabConfig.enabledTabs(UpgradeFeatureFlags(enableOneMonth = false))

        assertEquals(0, UpgradeTabConfig.indexOf(UpgradeTabKey.THREE_MONTHS, tabs))
        assertEquals(1, UpgradeTabConfig.indexOf(UpgradeTabKey.SIX_MONTHS, tabs))
        assertEquals(2, UpgradeTabConfig.indexOf(UpgradeTabKey.TILL_U_MARRY, tabs))
        assertEquals(3, UpgradeTabConfig.indexOf(UpgradeTabKey.PERSONALIZED, tabs))
        assertEquals(4, UpgradeTabConfig.indexOf(UpgradeTabKey.ELITE, tabs))
        assertEquals(5, UpgradeTabConfig.indexOf(UpgradeTabKey.TWIN_PACK, tabs))
        assertEquals(-1, UpgradeTabConfig.indexOf(UpgradeTabKey.ONE_MONTH, tabs))
    }

    @Test
    fun incomingCodesResolveBeforeIndexesAreCalculated() {
        val enabledTabs = UpgradeTabConfig.enabledTabs(UpgradeFeatureFlags(enableOneMonth = false))

        val tab = UpgradeRouteMapping.resolveLandingTabKey(
            landOnPage = 14,
            enabledTabs = enabledTabs
        )

        assertEquals(UpgradeTabKey.SIX_MONTHS, tab)
        assertEquals(1, UpgradeTabConfig.indexOf(tab, enabledTabs))
    }
}
