package com.soulmatch.app.data.upgrade

object UpgradeTabConfig {
    val canonicalOrder = listOf(
        UpgradeTabKey.SILVER,
        UpgradeTabKey.GOLD,
        UpgradeTabKey.PLATINUM
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
        return enabledTabs.firstOrNull() ?: UpgradeTabKey.SILVER
    }
}
