package com.soulmatch.app.ui.screens.subscription

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.soulmatch.app.data.upgrade.UpgradePackage
import com.soulmatch.app.data.upgrade.UpgradePackageGroup
import com.soulmatch.app.ui.components.premium.PremiumCard
import com.soulmatch.app.ui.formatCurrency
import com.soulmatch.app.ui.theme.Divider
import com.soulmatch.app.ui.theme.PrimaryDark
import com.soulmatch.app.ui.theme.SurfaceWarm
import com.soulmatch.app.ui.theme.TextSecondary

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun UpgradePackageCard(
    packageInfo: UpgradePackage,
    selected: Boolean,
    activePlanId: String,
    onSelect: () -> Unit,
    modifier: Modifier = Modifier
) {
    val active = activePlanId == packageInfo.planId
    val palette = membershipPalette(packageInfo.planId)
    PremiumCard(
        modifier = modifier.padding(horizontal = 16.dp, vertical = 8.dp),
        containerColor = palette.background,
        contentPadding = androidx.compose.foundation.layout.PaddingValues(18.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(
                imageVector = Icons.Filled.Star,
                contentDescription = null,
                tint = palette.accent,
                modifier = Modifier.size(30.dp)
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        packageInfo.displayName,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = palette.title,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        if (active) "CURRENT MEMBERSHIP" else packageInfo.badge?.ifBlank { "MONTHLY ACCESS" } ?: "MONTHLY ACCESS",
                        style = MaterialTheme.typography.labelSmall,
                        color = palette.body.copy(alpha = 0.82f),
                        fontWeight = FontWeight.Bold,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
                if (selected || active) {
                    Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = palette.accent)
                }
            }
            Text(
                formatCurrency(packageInfo.payableAmount),
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.ExtraBold,
                color = palette.title
            )
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                packageInfo.features.take(4).forEach { feature ->
                    Surface(
                        modifier = Modifier.weight(1f).heightIn(min = 74.dp),
                        shape = RoundedCornerShape(10.dp),
                        color = palette.tile,
                        border = BorderStroke(1.dp, palette.tileBorder)
                    ) {
                        Column(
                            modifier = Modifier.padding(10.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = palette.accent, modifier = Modifier.size(18.dp))
                            Text(
                                feature,
                                style = MaterialTheme.typography.labelMedium,
                                color = palette.body,
                                fontWeight = FontWeight.SemiBold,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
            if (packageInfo.pkgActualRate > packageInfo.payableAmount || packageInfo.savingsAmount > 0) {
                Text(
                    buildString {
                        if (packageInfo.pkgActualRate > packageInfo.payableAmount) append("${formatCurrency(packageInfo.pkgActualRate)}  ")
                        if (packageInfo.savingsAmount > 0) append("Save ${formatCurrency(packageInfo.savingsAmount)}")
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = palette.body.copy(alpha = 0.82f),
                    textDecoration = if (packageInfo.savingsAmount <= 0) TextDecoration.LineThrough else TextDecoration.None
                )
            }
            if (active) {
                OutlinedButton(onClick = {}, enabled = false, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
                    Text("Current membership")
                }
            } else {
                Button(onClick = onSelect, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
                    Text(if (selected) "Selected pack" else "Choose this pack")
                }
            }
        }
    }
}

private data class MembershipCardPalette(
    val background: Color,
    val title: Color,
    val body: Color,
    val accent: Color,
    val tile: Color,
    val tileBorder: Color
)

private fun membershipPalette(planId: String): MembershipCardPalette {
    return when (planId.lowercase()) {
        "gold" -> MembershipCardPalette(
            background = Color(0xFF950033),
            title = Color(0xFFFFE08A),
            body = Color.White,
            accent = Color(0xFFFFD86B),
            tile = Color(0x1AFFFFFF),
            tileBorder = Color(0x33FFFFFF)
        )
        "platinum" -> MembershipCardPalette(
            background = Color(0xFF273022),
            title = Color.White,
            body = Color.White,
            accent = Color(0xFFFFD86B),
            tile = Color(0x1AFFFFFF),
            tileBorder = Color(0x33FFFFFF)
        )
        "silver" -> MembershipCardPalette(
            background = Color(0xFFF7F4F2),
            title = PrimaryDark,
            body = PrimaryDark,
            accent = Color(0xFF9CA3AF),
            tile = Color.White,
            tileBorder = Divider
        )
        else -> MembershipCardPalette(
            background = SurfaceWarm,
            title = PrimaryDark,
            body = PrimaryDark,
            accent = Color(0xFFC17E6B),
            tile = Color.White,
            tileBorder = Divider
        )
    }
}

@Composable
fun UpgradeTabBanner(
    group: UpgradePackageGroup?,
    modifier: Modifier = Modifier
) {
    if (group == null) return
    PremiumCard(modifier = modifier.padding(horizontal = 16.dp, vertical = 8.dp), containerColor = SurfaceWarm) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(group.bannerTitle.ifBlank { group.tabTitle }, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold)
            Text(group.bannerText, style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
            if (group.assistiveContent.isNotBlank()) {
                Text(group.assistiveContent, style = MaterialTheme.typography.bodySmall, color = PrimaryDark)
            }
        }
    }
}

@Composable
fun CheckoutSummary(
    packageInfo: UpgradePackage,
    isProcessing: Boolean,
    isActive: Boolean,
    actionLabel: String = "Continue",
    onPay: () -> Unit,
    modifier: Modifier = Modifier
) {
    androidx.compose.material3.Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, Divider)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    if (isActive) "Current plan" else "Selected plan",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
                Text(packageInfo.displayName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(
                    "${formatCurrency(packageInfo.payableAmount)} | ${packageInfo.pkgDuration} | Secure checkout",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold
                )
            }
            if (isActive) {
                OutlinedButton(onClick = {}, enabled = false) {
                    Text("Active")
                }
            } else {
                Button(onClick = onPay, enabled = !isProcessing) {
                    Text(if (isProcessing) "Starting..." else actionLabel)
                }
            }
        }
    }
}
