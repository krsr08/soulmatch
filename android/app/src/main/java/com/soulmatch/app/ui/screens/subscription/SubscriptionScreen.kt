package com.soulmatch.app.ui.screens.subscription

import android.app.Activity
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.SupportAgent
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.razorpay.Checkout
import com.soulmatch.app.data.models.MonetizationRuntimeData
import com.soulmatch.app.data.models.PlanData
import com.soulmatch.app.BuildConfig
import com.soulmatch.app.data.models.SubscriptionData
import com.soulmatch.app.data.payments.PendingCheckout
import com.soulmatch.app.data.upgrade.UpgradePackage
import com.soulmatch.app.data.upgrade.UpgradePackageGroup
import com.soulmatch.app.data.upgrade.UpgradeTabConfig
import com.soulmatch.app.data.upgrade.UpgradeTabKey
import com.soulmatch.app.ui.components.premium.ChipTone
import com.soulmatch.app.ui.components.premium.PremiumCard
import com.soulmatch.app.ui.components.premium.PremiumScreen
import com.soulmatch.app.ui.components.premium.SignalChip
import com.soulmatch.app.ui.formatCurrency
import com.soulmatch.app.ui.theme.Divider
import com.soulmatch.app.ui.theme.PrimaryDark
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
    monetization: MonetizationRuntimeData = MonetizationRuntimeData(),
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
            if (BuildConfig.DEBUG) {
                vm.completeMockCheckout()
            } else {
                vm.failCheckout("SoulMatch payments are unavailable. Please try again later.")
            }
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
        ?: UpgradeTabKey.SILVER
    val selectedGroup = packageGroups.firstOrNull { it.semanticKey == selectedTab }
    val selectedPackage = packageGroups.asSequence()
        .flatMap { it.packages.asSequence() }
        .firstOrNull { it.pkgId == selectedPackageId }
        ?: selectedGroup?.packages?.firstOrNull()
    val paidPackages = packageGroups.flatMap { it.packages }
    val packageNameByPlanId = packageGroups
        .flatMap { it.packages }
        .associate { it.planId to it.displayName }
    val currentMembershipName = subscription.planName
        .takeIf { it.isNotBlank() }
        ?: packageNameByPlanId[subscription.planId]
        ?: canonicalPlanName(subscription.planId)
    val fixedPackage = remember(monetization) { monetization.fixedAccessPackage() }
    val checkoutPackage = if (monetization.isFixedPriceMode()) fixedPackage else selectedPackage

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
                title = {},
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            checkoutPackage?.let { pkg ->
                val showRenew = shouldShowRenew(subscription, pkg)
                CheckoutSummary(
                    packageInfo = pkg,
                    isProcessing = isProcessingPayment,
                    isActive = !monetization.isFixedPriceMode() && subscription.planId == pkg.planId && !showRenew,
                    actionLabel = checkoutActionLabel(subscription, pkg),
                    onPay = { vm.startCheckout(pkg) }
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
                    AccessModeNotice(monetization = monetization)
                }
                if (monetization.refundGuaranteeEnabled) {
                    item {
                        RefundGuaranteeBanner(monetization = monetization)
                    }
                }
                item {
                    CurrentMembershipCard(
                        subscription = subscription,
                        currentPlanName = currentMembershipName
                    )
                }
                item {
                    SectionHeading(
                        title = "Upgrade Membership",
                        subtitle = "Choose Pro, Pro Max, or Pro Supreme based on the access your family needs."
                    )
                }
                item {
                    MembershipComparisonGrid(
                        plans = monetization.plans,
                        matrix = monetization.membershipFeatureMatrix,
                        selectedPlanId = checkoutPackage?.planId ?: selectedPackage?.planId.orEmpty(),
                        activePlanId = subscription.planId,
                        onSelectPlan = { planId ->
                            packageGroups
                                .flatMap { it.packages }
                                .firstOrNull { it.planId.equals(planId, ignoreCase = true) }
                                ?.let { vm.selectPackage(it.pkgId) }
                        }
                    )
                }
                if (monetization.isSubscriptionMode()) {
                    items(paidPackages, key = { it.pkgId }) { packageInfo ->
                        UpgradePackageCard(
                            packageInfo = packageInfo,
                            selected = selectedPackageId == packageInfo.pkgId,
                            activePlanId = subscription.planId,
                            onSelect = { vm.selectPackage(packageInfo.pkgId) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionHeading(title: String, subtitle: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            title,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.ExtraBold,
            color = PrimaryDark
        )
        Text(
            subtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary
        )
    }
}

@Composable
private fun UpgradeModeSelector() {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, Divider)
    ) {
        Row(Modifier.padding(4.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            Surface(
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp),
                shape = RoundedCornerShape(12.dp),
                color = SurfaceWarm,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.48f))
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text("Self-Service", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold, color = PrimaryDark)
                }
            }
            Surface(
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp),
                shape = RoundedCornerShape(12.dp),
                color = Color.Transparent
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text("Assisted", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, color = TextSecondary)
                }
            }
        }
    }
}

@Composable
private fun AccessModeNotice(monetization: MonetizationRuntimeData) {
    if (monetization.isSubscriptionMode()) return
    PremiumCard(
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
        containerColor = if (monetization.isFreeAccessMode()) Color(0xFFEFFAF5) else Color(0xFFFFF4E7),
        contentPadding = PaddingValues(14.dp)
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(
                if (monetization.isFreeAccessMode()) Icons.Filled.CheckCircle else Icons.Filled.Info,
                contentDescription = null,
                tint = if (monetization.isFreeAccessMode()) Color(0xFF0F7A4F) else Color(0xFF9B5B00),
                modifier = Modifier.size(26.dp)
            )
            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(
                    if (monetization.isFreeAccessMode()) "Free access is enabled" else "Fixed price access",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.ExtraBold,
                    color = PrimaryDark
                )
                Text(
                    if (monetization.isFreeAccessMode()) {
                        "The backend currently allows all members to use paid features without checkout."
                    } else {
                        "A single ${monetization.fixedPriceLabel.ifBlank { formatCurrency(monetization.fixedPriceAmount) }} payment unlocks configured member access."
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
            }
        }
    }
}

@Composable
private fun RefundGuaranteeBanner(monetization: MonetizationRuntimeData) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        shape = RoundedCornerShape(16.dp),
        color = Color(0xFFE9F8EE),
        border = BorderStroke(1.dp, Color(0xFFC7E8D1))
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(modifier = Modifier.size(58.dp), shape = RoundedCornerShape(999.dp), color = Color(0xFF68C57F)) {
                Box(contentAlignment = Alignment.Center) {
                    Text("30\nDAY", style = MaterialTheme.typography.labelMedium, color = Color.White, fontWeight = FontWeight.ExtraBold, textAlign = TextAlign.Center)
                }
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(monetization.refundGuaranteeTitle, style = MaterialTheme.typography.titleMedium, color = Color(0xFF1F8D3B), fontWeight = FontWeight.ExtraBold)
                Text(monetization.refundGuaranteeSubtitle, style = MaterialTheme.typography.bodySmall, color = Color(0xFF1F8D3B))
            }
            Icon(Icons.Filled.Info, contentDescription = null, tint = Color(0xFF1F8D3B))
        }
    }
}

@Composable
private fun MembershipComparisonGrid(
    plans: List<PlanData>,
    matrix: List<Map<String, Any>>,
    selectedPlanId: String,
    activePlanId: String,
    onSelectPlan: (String) -> Unit
) {
    val tiers = plans.ifEmpty { fallbackMemberPlans() }
        .filter { it.planId.normalizedMemberPlanId() in CANONICAL_MEMBER_PLAN_IDS }
        .sortedBy { it.tierRank }
        .ifEmpty { fallbackMemberPlans() }
    val rows = matrix.ifEmpty { fallbackFeatureMatrix() }.take(7)
    PremiumCard(
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
        containerColor = MaterialTheme.colorScheme.surface,
        contentPadding = PaddingValues(14.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Text("Compare member benefits", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold, color = PrimaryDark)
            LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp), contentPadding = PaddingValues(end = 2.dp)) {
                items(tiers, key = { it.planId }) { plan ->
                    PlanTierColumn(
                        plan = plan,
                        rows = rows,
                        selected = selectedPlanId.equals(plan.planId, ignoreCase = true),
                        active = activePlanId.equals(plan.planId, ignoreCase = true),
                        onSelect = { onSelectPlan(plan.planId) }
                    )
                }
            }
        }
    }
}

@Composable
private fun PlanTierColumn(
    plan: PlanData,
    rows: List<Map<String, Any>>,
    selected: Boolean,
    active: Boolean,
    onSelect: () -> Unit
) {
    Surface(
        modifier = Modifier
            .width(148.dp)
            .defaultMinSize(minHeight = 360.dp)
            .clickable(onClick = onSelect),
        shape = RoundedCornerShape(18.dp),
        color = if (selected) Color(0xFFFFEEF2) else Color.White,
        border = BorderStroke(1.2.dp, if (selected) MaterialTheme.colorScheme.primary else Divider)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(plan.displayName.ifBlank { plan.name.ifBlank { titleCase(plan.planId) } }, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold, color = PrimaryDark, textAlign = TextAlign.Center)
            Icon(if (selected || active) Icons.Filled.CheckCircle else Icons.Filled.AutoAwesome, contentDescription = null, tint = if (selected || active) MaterialTheme.colorScheme.primary else Divider, modifier = Modifier.size(30.dp))
            rows.forEach { row ->
                val value = row.tierValue(plan.planId)
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(row["label"]?.toString().orEmpty(), style = MaterialTheme.typography.labelSmall, color = TextSecondary, textAlign = TextAlign.Center, maxLines = 2)
                    Text(formatFeatureValue(value), style = MaterialTheme.typography.titleSmall, color = PrimaryDark, fontWeight = FontWeight.ExtraBold, textAlign = TextAlign.Center, maxLines = 2)
                }
            }
        }
    }
}

@Composable
private fun DurationOfferStrip(
    groups: List<UpgradePackageGroup>,
    selectedTab: UpgradeTabKey,
    selectedPackage: UpgradePackage?,
    onSelectTab: (UpgradeTabKey) -> Unit
) {
    val visibleGroups = groups.ifEmpty { emptyList() }
    if (visibleGroups.isEmpty()) return
    Column(
        modifier = Modifier.padding(vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Spacer(Modifier.height(1.dp).weight(1f).background(Divider))
            Text("FLAT SAVINGS ON ALL PLANS", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.ExtraBold)
            Spacer(Modifier.height(1.dp).weight(1f).background(Divider))
        }
        LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            items(visibleGroups, key = { it.tabKey }) { group ->
                val key = group.semanticKey ?: return@items
                val plan = group.packages.firstOrNull { it.pkgId == selectedPackage?.pkgId } ?: group.packages.firstOrNull()
                Surface(
                    modifier = Modifier
                        .width(150.dp)
                        .height(92.dp)
                        .clickable { onSelectTab(key) },
                    shape = RoundedCornerShape(14.dp),
                    color = if (key == selectedTab) Color(0xFFFFEFF3) else Color.White,
                    border = BorderStroke(1.dp, if (key == selectedTab) MaterialTheme.colorScheme.primary else Divider)
                ) {
                    Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text(group.tabTitle.ifBlank { key.displayTitle }, style = MaterialTheme.typography.titleSmall, color = if (key == selectedTab) MaterialTheme.colorScheme.primary else PrimaryDark, fontWeight = FontWeight.ExtraBold)
                            Icon(if (key == selectedTab) Icons.Filled.CheckCircle else Icons.Filled.Info, contentDescription = null, tint = if (key == selectedTab) MaterialTheme.colorScheme.primary else Divider, modifier = Modifier.size(20.dp))
                        }
                        Text(plan?.payableAmount?.let(::formatCurrency) ?: "Configured", style = MaterialTheme.typography.titleMedium, color = PrimaryDark, fontWeight = FontWeight.ExtraBold)
                        plan?.pkgActualRate?.takeIf { it > plan.payableAmount }?.let {
                            Text(formatCurrency(it), style = MaterialTheme.typography.bodySmall, color = TextSecondary, textDecoration = TextDecoration.LineThrough)
                        }
                    }
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
        put("theme.color", "#FF5C00")
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

private fun MonetizationRuntimeData.isSubscriptionMode(): Boolean {
    return subscriptionModelEnabled || accessMode.equals("subscription", ignoreCase = true)
}

private fun MonetizationRuntimeData.isFreeAccessMode(): Boolean {
    return !subscriptionModelEnabled && accessMode.equals("free", ignoreCase = true)
}

private fun MonetizationRuntimeData.isFixedPriceMode(): Boolean {
    return !subscriptionModelEnabled && accessMode.equals("fixed_price", ignoreCase = true)
}

private fun MonetizationRuntimeData.fixedAccessPackage(): UpgradePackage {
    val amount = fixedPriceAmount.coerceAtLeast(1)
    return UpgradePackage(
        pkgId = 9001,
        remotePlanId = fixedPricePlanId.ifBlank { "fixed_access" },
        pkgName = "SoulMatch Fixed Access",
        pkgActualRate = amount,
        pkgDiscountedRate = amount,
        pkgRate = amount,
        pkgDuration = "30 days",
        pkgDurationDays = 30,
        pkgPhoneCount = 999,
        pkgBenefit = "Full member access at the configured fixed price.",
        buyerChoice = true,
        badge = "Fixed price",
        features = listOf("Best matches", "Interest actions", "Contact access rules", "Safety and trust tools")
    )
}

private val CANONICAL_MEMBER_PLAN_IDS = setOf("bronze", "silver", "gold", "platinum")

private fun String.normalizedMemberPlanId(): String {
    return when (lowercase(Locale.getDefault()).trim()) {
        "", "free" -> "bronze"
        else -> lowercase(Locale.getDefault()).trim()
    }
}

private fun fallbackMemberPlans(): List<PlanData> = listOf(
    PlanData("free", "Bronze", "Bronze", 0, "lifetime", 0, 0, listOf("80 visible matches", "10 profile views", "5 interests")),
    PlanData("silver", "Pro", "Pro", 199, "monthly", 30, 1, listOf("Contact sharing", "Engage+", "25 contact details", "Gold badge")),
    PlanData("gold", "Pro Max", "Pro Max", 399, "monthly", 30, 2, listOf("50 contact details", "50 Super Interests", "1 spotlight", "Gold badge")),
    PlanData("platinum", "Pro Supreme", "Pro Supreme", 599, "monthly", 30, 3, listOf("80 contact details", "80 Super Interests", "3 spotlights", "Gold badge"))
)

private fun fallbackFeatureMatrix(): List<Map<String, Any>> = listOf(
    mapOf("label" to "Contact Sharing", "free" to false, "bronze" to false, "silver" to true, "gold" to true, "platinum" to true),
    mapOf("label" to "Engage+", "free" to false, "bronze" to false, "silver" to true, "gold" to true, "platinum" to true),
    mapOf("label" to "Contact Details", "free" to "0", "bronze" to "0", "silver" to "25", "gold" to "50", "platinum" to "80"),
    mapOf("label" to "Super Interest", "free" to "0", "bronze" to "0", "silver" to "0", "gold" to "50", "platinum" to "80"),
    mapOf("label" to "Spotlights", "free" to "0", "bronze" to "0", "silver" to "0", "gold" to "1", "platinum" to "3"),
    mapOf("label" to "Gold Badge", "free" to false, "bronze" to false, "silver" to true, "gold" to true, "platinum" to true)
)

private fun Map<String, Any>.tierValue(planId: String): Any? {
    val normalized = planId.lowercase(Locale.getDefault()).ifBlank { "free" }
    return this[normalized] ?: this[if (normalized == "free") "bronze" else normalized]
}

private fun formatFeatureValue(value: Any?): String {
    return when (value) {
        is Boolean -> if (value) "✓" else "—"
        is Number -> if (value.toDouble() == 0.0) "0" else value.toString().removeSuffix(".0")
        null -> "—"
        else -> value.toString().ifBlank { "—" }
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

private fun canonicalPlanName(planId: String): String {
    return when (planId.lowercase(Locale.getDefault())) {
        "silver" -> "Pro"
        "gold" -> "Pro Max"
        "platinum" -> "Pro Supreme"
        "free", "bronze", "" -> "Bronze"
        else -> titleCase(planId.replace('_', ' '))
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
