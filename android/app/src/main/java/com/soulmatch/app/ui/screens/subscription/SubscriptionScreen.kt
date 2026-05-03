package com.soulmatch.app.ui.screens.subscription

import android.app.Activity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.razorpay.Checkout
import com.soulmatch.app.BuildConfig
import com.soulmatch.app.data.models.SubscriptionData
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
import com.soulmatch.app.ui.viewmodels.PaymentResultUi
import com.soulmatch.app.ui.viewmodels.PlanChangePrompt
import com.soulmatch.app.ui.viewmodels.PlanPromptType
import com.soulmatch.app.ui.viewmodels.SubscriptionViewModel
import org.json.JSONObject
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

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
    onPaymentResultDone: () -> Unit = {},
    vm: SubscriptionViewModel = hiltViewModel()
) {
    val packageGroups by vm.packageGroups.collectAsStateWithLifecycle()
    val subscription by vm.subscription.collectAsStateWithLifecycle()
    val selectedTabKey by vm.selectedTabKey.collectAsStateWithLifecycle()
    val selectedPackageId by vm.selectedPackageId.collectAsStateWithLifecycle()
    val checkoutRequest by vm.checkoutRequest.collectAsStateWithLifecycle()
    val planPrompt by vm.planPrompt.collectAsStateWithLifecycle()
    val paymentResult by vm.paymentResult.collectAsStateWithLifecycle()
    val isLoading by vm.isLoading.collectAsStateWithLifecycle()
    val isProcessingPayment by vm.isProcessingPayment.collectAsStateWithLifecycle()
    val errorMessage by vm.errorMessage.collectAsStateWithLifecycle()
    val statusMessage by vm.statusMessage.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = androidx.compose.ui.platform.LocalContext.current
    val planSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

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
        val keyId = resolvedRazorpayKeyId(pending, razorpayKeyId)
        if (!isUsableRazorpayKeyId(keyId)) {
            vm.failCheckout("SoulMatch payments are not configured correctly. Please try again later.")
            return@LaunchedEffect
        }
        try {
            Checkout()
                .apply { setKeyID(keyId) }
                .open(activity, checkoutOptions(pending, keyId))
            vm.markCheckoutConsumed()
        } catch (_: Exception) {
            vm.failCheckout("Payment checkout could not be started.")
        }
    }

    paymentResult?.let { result ->
        PaymentResultScreen(
            result = result,
            onOk = {
                vm.clearPaymentResult()
                onPaymentResultDone()
            }
        )
        return
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

    planPrompt?.let { prompt ->
        ModalBottomSheet(
            sheetState = planSheetState,
            onDismissRequest = { vm.dismissPlanPrompt() }
        ) {
            PlanChangeBottomSheet(
                prompt = prompt,
                onDismiss = { vm.dismissPlanPrompt() },
                onConfirm = { vm.confirmPlanPrompt() }
            )
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Membership") },
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
                val showRenew = shouldShowRenew(subscription, pkg)
                CheckoutSummary(
                    packageInfo = pkg,
                    isProcessing = isProcessingPayment,
                    isActive = subscription.planId == pkg.planId && !showRenew,
                    actionLabel = checkoutActionLabel(subscription, pkg),
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
                        subscription = subscription,
                        currentPlanName = packageNameByPlanId[subscription.planId]
                    )
                }
                item {
                    SectionTitle(
                        title = "Choose a plan",
                        subtitle = "Pick the support level that matches how actively your family is searching.",
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

private fun resolvedRazorpayKeyId(pending: PendingCheckout, publicKeyId: String): String {
    return pending.order.keyId.trim()
        .ifBlank { publicKeyId.trim() }
        .ifBlank { BuildConfig.RAZORPAY_KEY.trim() }
}

private fun isUsableRazorpayKeyId(keyId: String): Boolean {
    return keyId != "rzp_test_placeholder" &&
        (keyId.startsWith("rzp_test_") || keyId.startsWith("rzp_live_"))
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
    subscription: SubscriptionData,
    currentPlanName: String?
) {
    val currentPlanId = subscription.planId.ifBlank { "free" }
    val activePaid = subscription.isActive && currentPlanId != "free"
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
                Text(
                    if (activePaid && !subscription.endDate.isNullOrBlank()) {
                        "Valid until ${formatMembershipDate(subscription.endDate)}. Renewal opens in the final week."
                    } else {
                        "Start free, then upgrade only when you need more contact access and assisted discovery."
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )
            }
            SignalChip(if (activePaid) "Active" else "Free", tone = if (activePaid) ChipTone.Success else ChipTone.Neutral)
        }
    }
}

@Composable
private fun PlanChangeBottomSheet(
    prompt: PlanChangePrompt,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 22.dp, vertical = 18.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Surface(
            shape = androidx.compose.foundation.shape.CircleShape,
            color = if (prompt.type == PlanPromptType.UpgradeConfirm) SurfaceWarm else MaterialTheme.colorScheme.errorContainer,
            modifier = Modifier.size(52.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = if (prompt.type == PlanPromptType.UpgradeConfirm) Icons.Filled.Star else Icons.Filled.Error,
                    contentDescription = null,
                    tint = if (prompt.type == PlanPromptType.UpgradeConfirm) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onErrorContainer
                )
            }
        }
        Text(prompt.title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold)
        Text(prompt.message, style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
        Spacer(Modifier.height(4.dp))
        if (prompt.type == PlanPromptType.UpgradeConfirm) {
            Button(onClick = onConfirm, modifier = Modifier.fillMaxWidth()) {
                Text(prompt.confirmLabel ?: "Continue")
            }
            OutlinedButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) {
                Text("Not now")
            }
        } else {
            Button(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) {
                Text("OK")
            }
        }
        Spacer(Modifier.height(10.dp))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PaymentResultScreen(
    result: PaymentResultUi,
    onOk: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (result.success) "Payment successful" else "Payment failed") }
            )
        }
    ) { padding ->
        PremiumScreen(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(padding)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp),
                contentAlignment = Alignment.Center
            ) {
                PremiumCard(
                    modifier = Modifier.fillMaxWidth(),
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentPadding = PaddingValues(22.dp)
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Surface(
                            shape = androidx.compose.foundation.shape.CircleShape,
                            color = if (result.success) SurfaceWarm else MaterialTheme.colorScheme.errorContainer,
                            modifier = Modifier.size(72.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = if (result.success) Icons.Filled.CheckCircle else Icons.Filled.Error,
                                    contentDescription = null,
                                    tint = if (result.success) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onErrorContainer,
                                    modifier = Modifier.size(40.dp)
                                )
                            }
                        }
                        Text(result.title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.ExtraBold)
                        Text(result.message, style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                        result.detail?.takeIf { it.isNotBlank() }?.let {
                            Surface(
                                shape = androidx.compose.foundation.shape.RoundedCornerShape(10.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant
                            ) {
                                Text(
                                    it.take(180),
                                    modifier = Modifier.padding(12.dp),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Button(onClick = onOk, modifier = Modifier.fillMaxWidth()) {
                            Text("OK")
                        }
                    }
                }
            }
        }
    }
}

private fun checkoutActionLabel(subscription: SubscriptionData, packageInfo: UpgradePackage): String {
    val activePaid = subscription.isActive && subscription.planId.isNotBlank() && subscription.planId != "free"
    if (!activePaid) return "Continue"
    if (subscription.planId == packageInfo.planId) {
        return if (shouldShowRenew(subscription, packageInfo)) "Renew" else "Active"
    }
    val currentRank = planRank(subscription.planId, null)
    val targetRank = planRank(packageInfo.planId, packageInfo.payableAmount)
    return when {
        targetRank > currentRank -> "Upgrade"
        targetRank < currentRank -> "Unavailable"
        else -> "Continue"
    }
}

private fun shouldShowRenew(subscription: SubscriptionData, packageInfo: UpgradePackage): Boolean {
    if (!subscription.isActive || subscription.planId != packageInfo.planId) return false
    val daysLeft = daysUntilExpiry(subscription.endDate) ?: return false
    return daysLeft <= 7L
}

private fun planRank(planId: String, amount: Int?): Int {
    return when (planId.lowercase(Locale.getDefault())) {
        "free" -> 0
        "silver" -> 1
        "gold" -> 2
        "platinum" -> 3
        else -> ((amount ?: 0) / 500).coerceAtLeast(1)
    }
}

private fun daysUntilExpiry(value: String?): Long? {
    if (value.isNullOrBlank()) return null
    val expiry = runCatching { OffsetDateTime.parse(value) }
        .getOrElse {
            runCatching { OffsetDateTime.parse("${value}Z") }.getOrNull()
                ?: return null
        }
    val millis = expiry.toInstant().toEpochMilli() - System.currentTimeMillis()
    return kotlin.math.ceil(millis / 86400000.0).toLong().coerceAtLeast(0L)
}

private fun formatMembershipDate(value: String?): String {
    if (value.isNullOrBlank()) return "Not available"
    return runCatching {
        OffsetDateTime.parse(value).format(DateTimeFormatter.ofPattern("dd MMM yyyy", Locale.getDefault()))
    }.getOrElse {
        value.take(10).ifBlank { "Not available" }
    }
}
