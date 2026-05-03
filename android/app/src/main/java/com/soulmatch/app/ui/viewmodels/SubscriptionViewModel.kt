package com.soulmatch.app.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.soulmatch.app.data.api.PaymentApiService
import com.soulmatch.app.data.config.AppEnvironment
import com.soulmatch.app.data.local.UserPreferences
import com.soulmatch.app.data.mock.MarketFixtures
import com.soulmatch.app.data.models.InvoiceItem
import com.soulmatch.app.data.models.OrderData
import com.soulmatch.app.data.models.OrderRequest
import com.soulmatch.app.data.models.PaymentVerifyRequest
import com.soulmatch.app.data.models.SubscriptionData
import com.soulmatch.app.data.payments.PaymentCoordinator
import com.soulmatch.app.data.payments.PaymentOutcome
import com.soulmatch.app.data.payments.PendingCheckout
import com.soulmatch.app.data.upgrade.UpgradeFeatureFlags
import com.soulmatch.app.data.upgrade.UpgradeLandingArgs
import com.soulmatch.app.data.upgrade.UpgradePackage
import com.soulmatch.app.data.upgrade.UpgradePackageGroup
import com.soulmatch.app.data.upgrade.UpgradePackageRepository
import com.soulmatch.app.data.upgrade.UpgradeRouteMapping
import com.soulmatch.app.data.upgrade.UpgradeTabConfig
import com.soulmatch.app.data.upgrade.UpgradeTabKey
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.time.temporal.ChronoUnit

enum class PlanPromptType {
    UpgradeConfirm,
    DowngradeBlocked,
    EarlyRenewalBlocked
}

data class PlanChangePrompt(
    val type: PlanPromptType,
    val title: String,
    val message: String,
    val confirmLabel: String? = null
)

data class PaymentResultUi(
    val success: Boolean,
    val title: String,
    val message: String,
    val detail: String? = null
)

@HiltViewModel
class SubscriptionViewModel @Inject constructor(
    private val paymentApi: PaymentApiService,
    private val userPreferences: UserPreferences,
    private val paymentCoordinator: PaymentCoordinator,
    private val upgradePackageRepository: UpgradePackageRepository
) : ViewModel() {
    private val upgradeFeatureFlags = UpgradeFeatureFlags()
    private val _packageGroups = MutableStateFlow<List<UpgradePackageGroup>>(emptyList())
    private val _subscription = MutableStateFlow(SubscriptionData())
    private val _invoices = MutableStateFlow<List<InvoiceItem>>(emptyList())
    private val _selectedTabKey = MutableStateFlow(UpgradeTabKey.THREE_MONTHS.wireValue)
    private val _selectedPackageId = MutableStateFlow<Int?>(null)
    private val _isLoading = MutableStateFlow(false)
    private val _isProcessingPayment = MutableStateFlow(false)
    private val _checkoutRequest = MutableStateFlow<PendingCheckout?>(null)
    private val _planPrompt = MutableStateFlow<PlanChangePrompt?>(null)
    private val _paymentResult = MutableStateFlow<PaymentResultUi?>(null)
    private val _statusMessage = MutableStateFlow<String?>(null)
    private val _errorMessage = MutableStateFlow<String?>(null)
    private var landingArgs = UpgradeLandingArgs()
    private var hasConfiguredLanding = false

    val packageGroups: StateFlow<List<UpgradePackageGroup>> = _packageGroups.asStateFlow()
    val subscription: StateFlow<SubscriptionData> = _subscription.asStateFlow()
    val invoices: StateFlow<List<InvoiceItem>> = _invoices.asStateFlow()
    val selectedTabKey: StateFlow<String> = _selectedTabKey.asStateFlow()
    val selectedPackageId: StateFlow<Int?> = _selectedPackageId.asStateFlow()
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    val isProcessingPayment: StateFlow<Boolean> = _isProcessingPayment.asStateFlow()
    val checkoutRequest: StateFlow<PendingCheckout?> = _checkoutRequest.asStateFlow()
    val planPrompt: StateFlow<PlanChangePrompt?> = _planPrompt.asStateFlow()
    val paymentResult: StateFlow<PaymentResultUi?> = _paymentResult.asStateFlow()
    val statusMessage: StateFlow<String?> = _statusMessage.asStateFlow()
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    init {
        observePaymentResults()
    }

    private fun observePaymentResults() {
        viewModelScope.launch {
            paymentCoordinator.results.collect { result ->
                when (result) {
                    is PaymentOutcome.Success -> verifyPayment(result)
                    is PaymentOutcome.Failure -> {
                        _isProcessingPayment.value = false
                        _statusMessage.value = null
                        _errorMessage.value = null
                        _paymentResult.value = PaymentResultUi(
                            success = false,
                            title = "Payment not completed",
                            message = result.message.ifBlank {
                                "Payment failed. No membership was activated, and you can try again safely."
                            },
                            detail = result.rawResponse?.takeIf { it.isNotBlank() }
                        )
                    }
                }
            }
        }
    }

    fun configureLanding(landOnPage: Int?, routeCode: Int?, targetPackageId: String?) {
        val nextArgs = UpgradeLandingArgs(
            landOnPage = landOnPage,
            routeCode = routeCode,
            targetPackageId = targetPackageId?.takeIf { it.isNotBlank() }
        )
        if (hasConfiguredLanding && nextArgs == landingArgs) return
        landingArgs = nextArgs
        hasConfiguredLanding = true
        load(keepCurrentSelection = false)
    }

    fun load(keepCurrentSelection: Boolean = true) {
        viewModelScope.launch {
            _isLoading.value = true
            val savedRefreshPage = userPreferences.pageToLandAfterRefresh.first()
            val currentSelectedTab = _selectedTabKey.value.takeIf { keepCurrentSelection }
            val groups = UpgradeTabConfig.enabledGroups(
                groups = upgradePackageRepository.getPackageGroups(),
                flags = upgradeFeatureFlags
            )
            _packageGroups.value = groups

            _subscription.value = runCatching { paymentApi.getSubscription() }
                .getOrNull()
                ?.body()
                ?.takeIf { it.success }
                ?.data
                ?: if (AppEnvironment.allowDemoFallback) MarketFixtures.currentSubscription else SubscriptionData(planId = "free", isActive = false)
            _invoices.value = runCatching { paymentApi.getInvoices() }
                .getOrNull()
                ?.body()
                ?.takeIf { it.success }
                ?.data
                ?: if (AppEnvironment.allowDemoFallback) MarketFixtures.invoices else emptyList()

            resolveSelection(
                groups = groups,
                savedRefreshPage = currentSelectedTab ?: savedRefreshPage,
                preferRefreshState = keepCurrentSelection || !landingArgs.hasIncomingTarget
            )
            _isLoading.value = false
        }
    }

    fun selectTab(tabKey: String) {
        val semanticTab = UpgradeTabKey.from(tabKey) ?: return
        val group = groupForTab(semanticTab) ?: return
        _selectedTabKey.value = semanticTab.wireValue
        _selectedPackageId.value = preferredPackage(group)?.pkgId
        persistSelectedPage(semanticTab)
        clearMessages()
    }

    fun selectPackage(pkgId: Int) {
        val group = _packageGroups.value.firstOrNull { group ->
            group.packages.any { it.pkgId == pkgId }
        } ?: return
        val semanticTab = group.semanticKey ?: return
        _selectedTabKey.value = semanticTab.wireValue
        _selectedPackageId.value = pkgId
        persistSelectedPage(semanticTab)
        clearMessages()
    }

    fun dismissMessages() {
        clearMessages()
    }

    fun markCheckoutConsumed() {
        _checkoutRequest.value = null
    }

    fun completeMockCheckout() {
        val pending = _checkoutRequest.value ?: return
        _checkoutRequest.value = null
        viewModelScope.launch {
            userPreferences.savePlanId(pending.order.planId)
            _subscription.value = SubscriptionData(planId = pending.order.planId, isActive = true)
            _isProcessingPayment.value = false
            _paymentResult.value = PaymentResultUi(
                success = true,
                title = "Membership activated",
                message = "${pending.planName} membership is now active."
            )
        }
    }

    fun failCheckout(message: String) {
        _checkoutRequest.value = null
        _isProcessingPayment.value = false
        _errorMessage.value = message
    }

    fun dismissPlanPrompt() {
        _planPrompt.value = null
    }

    fun clearPaymentResult() {
        _paymentResult.value = null
    }

    fun confirmPlanPrompt() {
        val prompt = _planPrompt.value ?: return
        if (prompt.type != PlanPromptType.UpgradeConfirm) {
            _planPrompt.value = null
            return
        }
        val targetPackage = selectedPackage() ?: return
        _planPrompt.value = null
        beginCheckout(targetPackage)
    }

    fun startCheckout() {
        val targetPackage = selectedPackage() ?: return
        val current = _subscription.value
        val activePaidPlan = current.isActive && current.planId.isNotBlank() && current.planId != "free"
        if (activePaidPlan) {
            val currentRank = planRank(current.planId, null)
            val targetRank = planRank(targetPackage.planId, targetPackage.payableAmount)
            val daysLeft = daysUntilExpiry(current.endDate)
            val currentName = titleForPlan(current.planId)
            if (targetPackage.planId == current.planId) {
                if (daysLeft == null || daysLeft > RENEWAL_WINDOW_DAYS) {
                    _planPrompt.value = PlanChangePrompt(
                        type = PlanPromptType.EarlyRenewalBlocked,
                        title = "$currentName is already active",
                        message = "Renewal opens during the last $RENEWAL_WINDOW_DAYS days of your membership. You can continue using your current benefits now."
                    )
                    return
                }
            } else if (targetRank < currentRank) {
                _planPrompt.value = PlanChangePrompt(
                    type = PlanPromptType.DowngradeBlocked,
                    title = "Downgrade is not possible",
                    message = "Your $currentName plan is still active, so selecting a lower plan is blocked. Choose a higher plan or wait until your current plan expires."
                )
                return
            } else if (targetRank > currentRank) {
                _planPrompt.value = PlanChangePrompt(
                    type = PlanPromptType.UpgradeConfirm,
                    title = "Upgrade while current plan is active?",
                    message = "Your $currentName plan is still active. If you continue, SoulMatch will replace it with ${targetPackage.displayName} after successful payment.",
                    confirmLabel = "Continue upgrade"
                )
                return
            }
        }
        beginCheckout(targetPackage)
    }

    private fun beginCheckout(targetPackage: UpgradePackage) {
        if (targetPackage.planId == _subscription.value.planId) {
            val daysLeft = daysUntilExpiry(_subscription.value.endDate)
            if (daysLeft == null || daysLeft > RENEWAL_WINDOW_DAYS) {
                _statusMessage.value = "${targetPackage.displayName} is already active."
                return
            }
        }
        viewModelScope.launch {
            _isProcessingPayment.value = true
            clearMessages()
            val response = runCatching { paymentApi.createOrder(OrderRequest(targetPackage.planId)) }.getOrNull()
            val order = response
                ?.body()
                ?.takeIf { response.isSuccessful && it.success }
                ?.data
                ?: debugMockOrder(targetPackage)
            if (order == null) {
                _isProcessingPayment.value = false
                _errorMessage.value = orderStartFailureMessage(response?.body()?.error?.message)
                return@launch
            }
            val checkout = PendingCheckout(order = order, planName = targetPackage.displayName)
            paymentCoordinator.registerCheckout(checkout)
            _checkoutRequest.value = checkout
        }
    }

    private fun resolveSelection(
        groups: List<UpgradePackageGroup>,
        savedRefreshPage: String?,
        preferRefreshState: Boolean
    ) {
        val enabledTabs = groups.mapNotNull { it.semanticKey }
        val selectedTab = UpgradeRouteMapping.resolveLandingTabKey(
            landOnPage = landingArgs.landOnPage,
            routeCode = landingArgs.routeCode,
            targetPackageId = landingArgs.targetPackageId,
            pageToLandAfterRefresh = savedRefreshPage,
            enabledTabs = enabledTabs,
            preferRefreshState = preferRefreshState
        )
        _selectedTabKey.value = selectedTab.wireValue
        val selectedGroup = groups.firstOrNull { it.semanticKey == selectedTab }
        val targetPackageId = landingArgs.targetPackageId?.toIntOrNull()
        val currentPackageId = _selectedPackageId.value
        _selectedPackageId.value = when {
            targetPackageId != null && selectedGroup?.packages?.any { it.pkgId == targetPackageId } == true -> targetPackageId
            currentPackageId != null && selectedGroup?.packages?.any { it.pkgId == currentPackageId } == true -> currentPackageId
            else -> selectedGroup?.let(::preferredPackage)?.pkgId
        }
        persistSelectedPage(selectedTab)
    }

    private fun selectedPackage(): UpgradePackage? {
        val selectedId = _selectedPackageId.value ?: return null
        return _packageGroups.value.asSequence()
            .flatMap { it.packages.asSequence() }
            .firstOrNull { it.pkgId == selectedId }
    }

    private fun groupForTab(tabKey: UpgradeTabKey): UpgradePackageGroup? {
        return _packageGroups.value.firstOrNull { it.semanticKey == tabKey }
    }

    private fun preferredPackage(group: UpgradePackageGroup): UpgradePackage? {
        return group.packages.firstOrNull { it.buyerChoice || it.badge.equals("Recommended", ignoreCase = true) }
            ?: group.packages.firstOrNull()
    }

    private fun persistSelectedPage(tabKey: UpgradeTabKey) {
        viewModelScope.launch {
            userPreferences.savePageToLandAfterRefresh(tabKey.wireValue)
        }
    }

    private fun clearMessages() {
        _statusMessage.value = null
        _errorMessage.value = null
    }

    private fun debugMockOrder(targetPackage: UpgradePackage): OrderData? {
        if (!AppEnvironment.allowDemoFallback) return null
        return OrderData(
            orderId = "mock_order_${targetPackage.pkgId}_${System.currentTimeMillis()}",
            amount = targetPackage.payableAmount * 100,
            currency = "INR",
            planId = targetPackage.planId,
            gateway = "mock"
        )
    }

    private fun verifyPayment(result: PaymentOutcome.Success) {
        viewModelScope.launch {
            if (result.paymentId.isBlank() || result.signature.isBlank()) {
                _isProcessingPayment.value = false
                _statusMessage.value = null
                _errorMessage.value = null
                _paymentResult.value = PaymentResultUi(
                    success = false,
                    title = "Payment could not be confirmed",
                    message = "Payment completed, but Razorpay confirmation was incomplete. Please refresh your membership before trying again."
                )
                return@launch
            }
            val response = runCatching {
                paymentApi.verifyPayment(
                    PaymentVerifyRequest(
                        orderId = result.order.orderId,
                        paymentId = result.paymentId,
                        signature = result.signature,
                        planId = result.order.planId
                    )
                )
            }.getOrNull()
            if (response?.isSuccessful == true && response.body()?.success == true) {
                userPreferences.savePlanId(result.order.planId)
                _errorMessage.value = null
                _statusMessage.value = null
                _paymentResult.value = PaymentResultUi(
                    success = true,
                    title = "Payment successful",
                    message = "${result.planName} membership is now active."
                )
                load(keepCurrentSelection = true)
            } else {
                _statusMessage.value = null
                _errorMessage.value = null
                _paymentResult.value = PaymentResultUi(
                    success = false,
                    title = "Payment could not be confirmed",
                    message = paymentVerificationFailureMessage(response?.body()?.error?.message)
                )
            }
            _isProcessingPayment.value = false
        }
    }

    private fun orderStartFailureMessage(serverMessage: String?): String {
        val cleanMessage = serverMessage?.trim().orEmpty()
        if (cleanMessage.isNotBlank()) return cleanMessage
        return "Could not reach SoulMatch payments. Please check your connection and try again."
    }

    private fun paymentVerificationFailureMessage(serverMessage: String?): String {
        val cleanMessage = serverMessage?.trim().orEmpty()
        if (cleanMessage.isNotBlank()) {
            return "Payment could not be verified: $cleanMessage"
        }
        return "Payment completed, but SoulMatch could not confirm it. Please refresh your membership before paying again."
    }

    private fun planRank(planId: String, amount: Int?): Int {
        return when (planId.lowercase()) {
            "free" -> 0
            "silver" -> 1
            "gold" -> 2
            "platinum" -> 3
            else -> ((amount ?: 0) / 500).coerceAtLeast(1)
        }
    }

    private fun titleForPlan(planId: String): String {
        return _packageGroups.value
            .flatMap { it.packages }
            .firstOrNull { it.planId == planId }
            ?.displayName
            ?: when (planId.lowercase()) {
                "silver" -> "Silver"
                "gold" -> "Gold"
                "platinum" -> "Platinum"
                else -> planId.replaceFirstChar { it.titlecase() }
            }
    }

    private fun daysUntilExpiry(endDate: String?): Long? {
        if (endDate.isNullOrBlank()) return null
        val expiry = runCatching { OffsetDateTime.parse(endDate) }
            .getOrElse {
                runCatching { OffsetDateTime.parse("${endDate}Z") }.getOrNull()
                    ?: return null
            }
        return ChronoUnit.DAYS.between(OffsetDateTime.now(ZoneOffset.UTC), expiry).coerceAtLeast(0)
    }

    companion object {
        private const val RENEWAL_WINDOW_DAYS = 7L
    }
}
