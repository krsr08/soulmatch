package com.soulmatch.app.ui.screens.subscription

import android.app.Activity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.razorpay.Checkout
import com.soulmatch.app.BuildConfig
import com.soulmatch.app.data.models.InvoiceItem
import com.soulmatch.app.data.payments.PendingCheckout
import com.soulmatch.app.data.upgrade.UpgradePackage
import com.soulmatch.app.data.upgrade.UpgradePackageGroup
import com.soulmatch.app.data.upgrade.UpgradeTabConfig
import com.soulmatch.app.data.upgrade.UpgradeTabKey
import com.soulmatch.app.ui.components.ChipTone
import com.soulmatch.app.ui.components.PremiumCard
import com.soulmatch.app.ui.components.PremiumScreen
import com.soulmatch.app.ui.components.SectionTitle
import com.soulmatch.app.ui.components.SignalChip
import com.soulmatch.app.ui.formatCurrency
import com.soulmatch.app.ui.theme.Divider
import com.soulmatch.app.ui.theme.SurfaceWarm
import com.soulmatch.app.ui.theme.TextSecondary
import com.soulmatch.app.ui.titleCase
import com.soulmatch.app.ui.viewmodels.SubscriptionViewModel
import org.json.JSONObject

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubscriptionScreen(
    onBack: () -> Unit = {},
    onViewProfile: ((String) -> Unit)? = null,
    onOpenChat: ((String, String) -> Unit)? = null,
    onSubscribe: (() -> Unit)? = null,
    onEditSection: ((Int) -> Unit)? = null,
    onLogout: (() -> Unit)? = null,
    profileId: String = "",
    chatId: String = "",
    participantName: String = "",
    razorpayKeyId: String = "",
    landOnPage: Int? = null,
    routeCode: Int? = null,
    targetPackageId: String? = null,
    vm: SubscriptionViewModel = hiltViewModel()
) {
    val packageGroups by vm.packageGroups.collectAsStateWithLifecycle()
    val subscription by vm.subscription.collectAsStateWithLifecycle()
    val invoices by vm.invoices.collectAsStateWithLifecycle()
    val selectedTabKey by vm.selectedTabKey.collectAsStateWithLifecycle()
    val selectedPackageId by vm.selectedPackageId.collectAsStateWithLifecycle()
    val checkoutRequest by vm.checkoutRequest.collectAsStateWithLifecycle()
    val isLoading by vm.isLoading.collectAsStateWithLifecycle()
    val isProcessingPayment by vm.isProcessingPayment.collectAsStateWithLifecycle()
    val errorMessage by vm.errorMessage.collectAsStateWithLifecycle()
    val statusMessage by vm.statusMessage.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = androidx.compose.ui.platform.LocalContext.current

    LaunchedEffect(landOnPage, routeCode, targetPackageId) {
        vm.configureLanding(landOnPage = landOnPage, routeCode = routeCode, targetPackageId = targetPackageId)
    }

    LaunchedEffect(errorMessage, statusMessage) {
        val message = errorMessage ?: statusMessage
        if (!message.isNullOrBlank()) {
            snackbarHostState.showSnackbar(message)
            vm.dismissMessages()
        }
    }

    LaunchedEffect(checkoutRequest?.order?.orderId) {
        val pending = checkoutRequest ?: return@LaunchedEffect
        if (pending.order.gateway == "mock") {
            vm.completeMockCheckout()
            return@LaunchedEffect
        }
        val activity = context as? Activity
        if (activity == null) {
            vm.failCheckout("Could not open the payment window.")
            return@LaunchedEffect
        }
        try {
            Checkout().open(activity, checkoutOptions(pending, razorpayKeyId))
            vm.markCheckoutConsumed()
        } catch (_: Exception) {
            vm.failCheckout("Payment checkout could not be started.")
        }
    }

    val enabledTabKeys = packageGroups.mapNotNull { it.semanticKey }
    val selectedTab = UpgradeTabKey.from(selectedTabKey)
        ?.takeIf { it in enabledTabKeys }
        ?: enabledTabKeys.firstOrNull()
        ?: UpgradeTabKey.THREE_MONTHS
    val selectedGroup = packageGroups.firstOrNull { it.semanticKey == selectedTab }
    val selectedPackage = packageGroups.asSequence()
        .flatMap { it.packages.asSequence() }
        .firstOrNull { it.pkgId == selectedPackageId }
        ?: selectedGroup?.packages?.firstOrNull()
    val selectedTabIndex = UpgradeTabConfig.indexOf(selectedTab, enabledTabKeys).let { if (it >= 0) it else 0 }
    val packageNameByPlanId = packageGroups
        .flatMap { it.packages }
        .associate { it.planId to it.displayName }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Upgrade membership") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            selectedPackage?.let { pkg ->
                CheckoutSummary(
                    packageInfo = pkg,
                    isProcessing = isProcessingPayment,
                    isActive = subscription.planId == pkg.planId,
                    onPay = { vm.startCheckout() }
                )
            }
        }
    ) { padding ->
        PremiumScreen(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(padding)
        ) {
            if (isLoading && packageGroups.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
                return@PremiumScreen
            }
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 118.dp)
            ) {
                item {
                    MembershipHero(
                        selectedGroup = selectedGroup,
                        selectedPackage = selectedPackage,
                        currentPlanName = packageNameByPlanId[subscription.planId] ?: titleCase(subscription.planId)
                    )
                }
                item {
                    CurrentMembershipCard(
                        currentPlanId = subscription.planId,
                        currentPlanName = packageNameByPlanId[subscription.planId]
                    )
                }
                item {
                    SectionTitle(
                        title = "Choose membership",
                        subtitle = "Tabs, offers, pricing, and benefits are driven by the control panel package config.",
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                    )
                }
                item {
                    if (enabledTabKeys.isNotEmpty()) {
                        ScrollableTabRow(
                            selectedTabIndex = selectedTabIndex,
                            edgePadding = 12.dp,
                            containerColor = Color.Transparent
                        ) {
                            enabledTabKeys.forEach { tabKey ->
                                Tab(
                                    selected = tabKey == selectedTab,
                                    onClick = { vm.selectTab(tabKey.wireValue) },
                                    text = {
                                        Text(
                                            tabKey.displayTitle,
                                            fontWeight = if (tabKey == selectedTab) FontWeight.ExtraBold else FontWeight.SemiBold
                                        )
                                    }
                                )
                            }
                        }
                    }
                }
                item {
                    UpgradeTabBanner(group = selectedGroup)
                }
                items(selectedGroup?.packages.orEmpty(), key = { it.pkgId }) { packageInfo ->
                    UpgradePackageCard(
                        packageInfo = packageInfo,
                        selected = selectedPackageId == packageInfo.pkgId,
                        activePlanId = subscription.planId,
                        onSelect = { vm.selectPackage(packageInfo.pkgId) }
                    )
                }
                item {
                    UpgradeBenefitPanel(packageInfo = selectedPackage)
                }
                item {
                    BillingHistoryCard(invoices = invoices, packageNameByPlanId = packageNameByPlanId)
                }
            }
        }
    }
}

@Composable
private fun MembershipHero(
    selectedGroup: UpgradePackageGroup?,
    selectedPackage: UpgradePackage?,
    currentPlanName: String
) {
    PremiumCard(
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
        containerColor = SurfaceWarm,
        contentPadding = PaddingValues(18.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("SOULMATCH MEMBERSHIP", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.ExtraBold)
                    Text(
                        selectedGroup?.bannerTitle?.takeIf { it.isNotBlank() } ?: "Find the right plan for your family",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Text(
                        selectedGroup?.bannerText?.takeIf { it.isNotBlank() }
                            ?: "Unlock verified contact views, stronger visibility, and assisted discovery.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )
                }
                Surface(shape = androidx.compose.foundation.shape.CircleShape, color = MaterialTheme.colorScheme.primary, modifier = Modifier.size(48.dp)) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Filled.Star, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimary)
                    }
                }
            }
            selectedPackage?.let { pkg ->
                Surface(
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = androidx.compose.foundation.BorderStroke(1.dp, Divider.copy(alpha = 0.7f))
                ) {
                    Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                Text(pkg.displayName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold)
                                Text("${pkg.pkgDuration} | ${pkg.pkgPhoneCount} verified contact views", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text(formatCurrency(pkg.payableAmount), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.primary)
                                pkg.perDayAmount()?.let {
                                    Text("${formatCurrency(it)}/day", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                                }
                            }
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                            SignalChip(currentPlanName, tone = ChipTone.Neutral)
                            pkg.badge?.takeIf { it.isNotBlank() }?.let { SignalChip(it, tone = ChipTone.Gold) }
                        }
                    }
                }
            }
        }
    }
}

private fun checkoutOptions(pending: PendingCheckout, razorpayKeyId: String): JSONObject {
    return JSONObject().apply {
        put("name", "SoulMatch")
        put("description", "${pending.planName} membership")
        put("theme.color", "#D4285A")
        put("currency", pending.order.currency)
        put("amount", pending.order.amount)
        put("order_id", pending.order.orderId)
        put("key", pending.order.keyId.ifBlank { razorpayKeyId.ifBlank { BuildConfig.RAZORPAY_KEY } })
    }
}

@Composable
private fun CurrentMembershipCard(
    currentPlanId: String,
    currentPlanName: String?
) {
    PremiumCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        containerColor = SurfaceWarm
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Text("Current membership", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(
                    currentPlanName ?: titleCase(currentPlanId),
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.ExtraBold
                )
                Text("Your active membership and billing history stay available while you compare upgrades.", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
            }
            SignalChip(if (currentPlanId == "free") "Free" else "Active", tone = if (currentPlanId == "free") ChipTone.Neutral else ChipTone.Success)
        }
    }
}

@Composable
private fun BillingHistoryCard(
    invoices: List<InvoiceItem>,
    packageNameByPlanId: Map<String, String>
) {
    PremiumCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Billing history", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            if (invoices.isEmpty()) {
                Text("No successful payments yet.", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
            } else {
                invoices.forEach { invoice -> BillingRow(invoice, packageNameByPlanId[invoice.planId]) }
            }
        }
    }
}

@Composable
private fun BillingRow(invoice: InvoiceItem, packageName: String?) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(packageName ?: titleCase(invoice.planId), fontWeight = FontWeight.SemiBold)
        Text("${formatCurrency(invoice.amount.toInt())} | ${invoice.createdAt.take(10)}", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
    }
}
