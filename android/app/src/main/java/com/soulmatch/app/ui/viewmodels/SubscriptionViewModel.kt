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
                        _errorMessage.value = result.message
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
            _statusMessage.value = "${pending.planName} membership is now active in mock checkout."
        }
    }

    fun failCheckout(message: String) {
        _checkoutRequest.value = null
        _isProcessingPayment.value = false
        _errorMessage.value = message
    }

    fun startCheckout() {
        val targetPackage = selectedPackage() ?: return
        if (targetPackage.planId == _subscription.value.planId) {
            _statusMessage.value = "${targetPackage.pkgName} is already active."
            return
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
                _errorMessage.value = response?.body()?.error?.message ?: "Could not start payment."
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
                _statusMessage.value = "${result.planName} membership is now active."
                load(keepCurrentSelection = true)
            } else {
                _errorMessage.value = response?.body()?.error?.message ?: "Payment verification failed."
            }
            _isProcessingPayment.value = false
        }
    }
}
