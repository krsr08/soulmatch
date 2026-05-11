package com.soulmatch.app.ui.screens.subscription

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.LocalOffer
import androidx.compose.material.icons.filled.Phone
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.soulmatch.app.data.upgrade.UpgradePackage
import com.soulmatch.app.data.upgrade.UpgradePackageGroup
import com.soulmatch.app.ui.components.premium.ChipTone
import com.soulmatch.app.ui.components.premium.PremiumCard
import com.soulmatch.app.ui.components.premium.SignalChip
import com.soulmatch.app.ui.formatCurrency
import com.soulmatch.app.ui.theme.Divider
import com.soulmatch.app.ui.theme.InfoSoft
import com.soulmatch.app.ui.theme.PrimaryDark
import com.soulmatch.app.ui.theme.Success
import com.soulmatch.app.ui.theme.SurfaceSoft
import com.soulmatch.app.ui.theme.SurfaceWarm
import com.soulmatch.app.ui.theme.TextSecondary
import com.soulmatch.app.ui.theme.Warning
import com.soulmatch.app.ui.theme.WarningSoft

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
    PremiumCard(
        modifier = modifier.padding(horizontal = 16.dp, vertical = 8.dp),
        containerColor = if (selected || active) SurfaceWarm else MaterialTheme.colorScheme.surface
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalAlignment = Alignment.Top
            ) {
                AsyncImage(
                    model = packageInfo.choiceImage,
                    contentDescription = null,
                    modifier = Modifier
                        .size(width = 96.dp, height = 88.dp)
                        .clip(RoundedCornerShape(14.dp)),
                    contentScale = ContentScale.Crop
                )
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top
                    ) {
                        Text(
                            packageInfo.displayName,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                        if (selected || active) {
                            Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        packageInfo.badge?.takeIf { it.isNotBlank() }?.let { badge ->
                            SignalChip(badge, tone = if (packageInfo.buyerChoice) ChipTone.Gold else ChipTone.Info)
                        }
                        if (packageInfo.buyerChoice) {
                            SignalChip("Buyer Choice", tone = ChipTone.Success)
                        }
                    }
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text(
                        formatCurrency(packageInfo.payableAmount),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        if (packageInfo.pkgActualRate > packageInfo.payableAmount) {
                            Text(
                                formatCurrency(packageInfo.pkgActualRate),
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary,
                                textDecoration = TextDecoration.LineThrough
                            )
                        }
                        if (packageInfo.savingsAmount > 0) {
                            Text(
                                "Save ${formatCurrency(packageInfo.savingsAmount)}",
                                style = MaterialTheme.typography.labelMedium,
                                color = Success,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
                Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    packageInfo.perDayAmount()?.let {
                        Text("${formatCurrency(it)}/day", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                    }
                    packageInfo.perMonthAmount()?.let {
                        Text("${formatCurrency(it)}/month", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                    }
                }
            }
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                SignalChip(packageInfo.pkgDuration, tone = ChipTone.Neutral)
                SignalChip("${packageInfo.pkgPhoneCount} contacts", tone = ChipTone.Warm)
                packageInfo.toiPkgAmount?.let { SignalChip("TOI ${formatCurrency(it)}", tone = ChipTone.Gold) }
            }
            packageInfo.features.take(4).forEach { feature ->
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.Top) {
                    Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = Success, modifier = Modifier.size(18.dp))
                    Text(feature, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
                }
            }
            if (active) {
                OutlinedButton(onClick = {}, enabled = false, modifier = Modifier.fillMaxWidth()) {
                    Text("Current membership")
                }
            } else {
                Button(onClick = onSelect, modifier = Modifier.fillMaxWidth()) {
                    Text(if (selected) "Selected plan" else "Choose this plan")
                }
            }
        }
    }
}

@Composable
fun UpgradeBenefitPanel(
    packageInfo: UpgradePackage?,
    modifier: Modifier = Modifier
) {
    if (packageInfo == null) return
    PremiumCard(modifier = modifier.padding(horizontal = 16.dp, vertical = 8.dp), containerColor = MaterialTheme.colorScheme.surface) {
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Package benefits", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold)
                    Text(packageInfo.displayName, style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                }
                    Surface(shape = RoundedCornerShape(8.dp), color = WarningSoft, border = BorderStroke(1.dp, Divider)) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Filled.Star, contentDescription = null, tint = Warning, modifier = Modifier.size(18.dp))
                        Text("Selected", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = PrimaryDark)
                    }
                }
            }
            AsyncImage(
                model = packageInfo.pkgBenefitImg,
                contentDescription = null,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 8f)
                    .clip(RoundedCornerShape(8.dp)),
                contentScale = ContentScale.Crop
            )
            Text(packageInfo.pkgBenefit, style = MaterialTheme.typography.bodyMedium)
            if (packageInfo.linearContent.isNotBlank()) {
                Surface(shape = RoundedCornerShape(8.dp), color = SurfaceSoft, border = BorderStroke(1.dp, Divider)) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Icon(Icons.Filled.LocalOffer, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                        Text(packageInfo.linearContent, style = MaterialTheme.typography.bodyMedium, color = PrimaryDark)
                    }
                }
            }
            if (packageInfo.assistiveContent.isNotBlank()) {
                Surface(shape = RoundedCornerShape(8.dp), color = InfoSoft, border = BorderStroke(1.dp, Divider)) {
                    Text(
                        packageInfo.assistiveContent,
                        modifier = Modifier.padding(14.dp),
                        style = MaterialTheme.typography.bodySmall,
                        color = PrimaryDark
                    )
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.Phone, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Text("${packageInfo.pkgPhoneCount} verified contact views included", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
            }
        }
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
                    if (isActive) "Current package" else "Selected package",
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
