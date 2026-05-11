package com.soulmatch.app.ui.screens.subscription

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.soulmatch.app.data.models.InvoiceItem
import com.soulmatch.app.ui.components.premium.ChipTone
import com.soulmatch.app.ui.components.premium.PremiumCard
import com.soulmatch.app.ui.components.premium.PremiumScreen
import com.soulmatch.app.ui.components.premium.SignalChip
import com.soulmatch.app.ui.formatCurrency
import com.soulmatch.app.ui.theme.Divider
import com.soulmatch.app.ui.theme.SurfaceWarm
import com.soulmatch.app.ui.theme.TextSecondary
import com.soulmatch.app.ui.titleCase
import com.soulmatch.app.ui.viewmodels.SubscriptionViewModel
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubscriptionHistoryScreen(
    onBack: () -> Unit,
    vm: SubscriptionViewModel = hiltViewModel()
) {
    val invoices by vm.invoices.collectAsStateWithLifecycle()
    val packages by vm.packageGroups.collectAsStateWithLifecycle()
    val subscription by vm.subscription.collectAsStateWithLifecycle()
    val isLoading by vm.isLoading.collectAsStateWithLifecycle()
    var expandedId by rememberSaveable { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        vm.load()
    }

    val packageNameByPlanId = packages
        .flatMap { it.packages }
        .associate { it.planId to it.displayName }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Subscription history") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        PremiumScreen(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(padding)
        ) {
            if (isLoading && invoices.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
                return@PremiumScreen
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    PremiumCard(containerColor = SurfaceWarm) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("Membership records", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold)
                            Text(
                                "Every paid plan, transaction reference, method, status, and validity period is listed here for support and proof.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextSecondary
                            )
                        }
                    }
                }
                if (invoices.isEmpty()) {
                    item {
                        FreeMembershipHistoryRow(
                            currentPlan = subscription.planName.takeIf { it.isNotBlank() }
                                ?: packageNameByPlanId[subscription.planId]
                                ?: canonicalHistoryPlanName(subscription.planId),
                            expanded = expandedId == "free",
                            onClick = { expandedId = if (expandedId == "free") null else "free" }
                        )
                    }
                } else {
                    items(invoices, key = { it.transactionId.ifBlank { it.razorpayPaymentId } }) { invoice ->
                        val rowId = invoice.transactionId.ifBlank { invoice.razorpayPaymentId }
                        SubscriptionHistoryRow(
                            invoice = invoice,
                            packageName = packageNameByPlanId[invoice.planId],
                            expanded = expandedId == rowId,
                            onClick = { expandedId = if (expandedId == rowId) null else rowId }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun FreeMembershipHistoryRow(
    currentPlan: String,
    expanded: Boolean,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, Divider.copy(alpha = 0.65f))
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            HistoryRowHeader(
                title = if (currentPlan.equals("free", ignoreCase = true)) "Free membership" else currentPlan,
                subtitle = "No paid transactions yet",
                status = "Free",
                tone = ChipTone.Neutral,
                expanded = expanded
            )
            Text(formatCurrency(0), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.primary)
            if (expanded) {
                HistoryDetail("Membership", "Free")
                HistoryDetail("Status", "Active")
                HistoryDetail("Paid using", "No payment required")
                HistoryDetail("Transaction ID", "Not applicable")
            }
        }
    }
}

@Composable
private fun SubscriptionHistoryRow(
    invoice: InvoiceItem,
    packageName: String?,
    expanded: Boolean,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, Divider.copy(alpha = 0.65f))
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            HistoryRowHeader(
                title = invoice.planName.ifBlank { packageName ?: titleCase(invoice.planId) },
                subtitle = "Paid on ${formatHistoryDate(invoice.createdAt)}",
                status = paymentStatusLabel(invoice),
                tone = paymentStatusTone(invoice),
                expanded = expanded
            )
            Text(
                "${formatCurrency(invoice.amount.toInt())} ${invoice.currency.ifBlank { "INR" }}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.primary
            )
            if (expanded) {
                HistoryDetail("Paid using", paidUsingLabel(invoice))
                HistoryDetail("Transaction ID", invoice.razorpayPaymentId.ifBlank { invoice.transactionId })
                HistoryDetail("Order ID", invoice.razorpayOrderId.ifBlank { invoice.paymentOrderId })
                HistoryDetail("Gateway", invoice.gateway.ifBlank { "Razorpay" })
                HistoryDetail("Provider status", invoice.providerStatus.ifBlank { invoice.paymentOrderStatus.ifBlank { invoice.status } })
                if (invoice.startDate.isNotBlank() || invoice.endDate.isNotBlank()) {
                    HistoryDetail("Membership validity", "${formatHistoryDate(invoice.startDate)} to ${formatHistoryDate(invoice.endDate)}")
                }
                invoice.durationDays?.let { HistoryDetail("Duration", "$it days") }
            }
        }
    }
}

@Composable
private fun HistoryRowHeader(
    title: String,
    subtitle: String,
    status: String,
    tone: ChipTone,
    expanded: Boolean
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.Top
    ) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
        }
        SignalChip(status, tone = tone)
        Icon(
            imageVector = if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
            contentDescription = null,
            tint = TextSecondary
        )
    }
}

@Composable
private fun HistoryDetail(label: String, value: String) {
    if (value.isBlank()) return
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = TextSecondary)
        Text(value, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface)
    }
}

private fun paidUsingLabel(invoice: InvoiceItem): String {
    val gateway = invoice.gateway.ifBlank { "Razorpay" }.replaceFirstChar { it.titlecase(Locale.getDefault()) }
    val method = invoice.paymentMethod.ifBlank { "payment" }.replaceFirstChar { it.titlecase(Locale.getDefault()) }
    return listOf(gateway, method, invoice.paymentInstrument)
        .filter { it.isNotBlank() }
        .joinToString(" | ")
}

private fun paymentStatusLabel(invoice: InvoiceItem): String {
    return invoice.providerStatus
        .ifBlank { invoice.paymentOrderStatus }
        .ifBlank { invoice.status }
        .ifBlank { if (invoice.isActive) "active" else "updated" }
        .replace('_', ' ')
        .replaceFirstChar { it.titlecase(Locale.getDefault()) }
}

private fun paymentStatusTone(invoice: InvoiceItem): ChipTone {
    val status = paymentStatusLabel(invoice).lowercase(Locale.getDefault())
    return when {
        status.contains("success") || status.contains("paid") || status.contains("captured") || status.contains("active") -> ChipTone.Success
        status.contains("fail") || status.contains("declin") || status.contains("cancel") -> ChipTone.Warm
        else -> ChipTone.Neutral
    }
}

private fun canonicalHistoryPlanName(planId: String): String {
    return when (planId.lowercase(Locale.getDefault())) {
        "silver" -> "SoulMatch Verified Plus"
        "gold" -> "SoulMatch Family Assist"
        "platinum" -> "SoulMatch Platinum"
        "free", "" -> "Free"
        else -> titleCase(planId.replace('_', ' '))
    }
}

private fun formatHistoryDate(value: String): String {
    if (value.isBlank()) return "Not available"
    return runCatching {
        OffsetDateTime.parse(value).format(DateTimeFormatter.ofPattern("dd MMM yyyy", Locale.getDefault()))
    }.getOrElse {
        value.take(10).ifBlank { "Not available" }
    }
}
