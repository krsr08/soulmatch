package com.soulmatch.app.data.upgrade

object UpgradeTabConfig {
    val canonicalOrder = listOf(
        UpgradeTabKey.ONE_MONTH,
        UpgradeTabKey.THREE_MONTHS,
        UpgradeTabKey.SIX_MONTHS,
        UpgradeTabKey.TILL_U_MARRY,
        UpgradeTabKey.PERSONALIZED,
        UpgradeTabKey.ELITE,
        UpgradeTabKey.TWIN_PACK
    )

    fun enabledTabs(flags: UpgradeFeatureFlags = UpgradeFeatureFlags()): List<UpgradeTabKey> {
        return canonicalOrder.filter(flags::isEnabled)
    }

    fun enabledGroups(
        groups: List<UpgradePackageGroup>,
        flags: UpgradeFeatureFlags = UpgradeFeatureFlags()
    ): List<UpgradePackageGroup> {
        val byKey = groups.mapNotNull { group ->
            val key = group.semanticKey ?: return@mapNotNull null
            key to group
        }.toMap()
        return enabledTabs(flags).mapNotNull { key ->
            byKey[key]?.takeIf { it.packages.isNotEmpty() }
        }
    }

    fun indexOf(tabKey: UpgradeTabKey, enabledTabs: List<UpgradeTabKey>): Int {
        return enabledTabs.indexOf(tabKey)
    }

    fun firstAvailable(enabledTabs: List<UpgradeTabKey>): UpgradeTabKey {
        return enabledTabs.firstOrNull() ?: UpgradeTabKey.THREE_MONTHS
    }
}
