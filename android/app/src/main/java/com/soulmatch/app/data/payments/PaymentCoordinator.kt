package com.soulmatch.app.data.payments

import com.soulmatch.app.data.models.OrderData
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

data class PendingCheckout(
    val order: OrderData,
    val planName: String
)

sealed class PaymentOutcome {
    data class Success(
        val order: OrderData,
        val planName: String,
        val paymentId: String,
        val signature: String
    ) : PaymentOutcome()

    data class Failure(
        val order: OrderData?,
        val message: String,
        val code: Int? = null,
        val rawResponse: String? = null
    ) : PaymentOutcome()
}

@Singleton
class PaymentCoordinator @Inject constructor() {
    private var pendingCheckout: PendingCheckout? = null
    private val _results = MutableSharedFlow<PaymentOutcome>(
        replay = 0,
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    val results = _results.asSharedFlow()

    fun registerCheckout(checkout: PendingCheckout) {
        pendingCheckout = checkout
    }

    suspend fun completeSuccess(paymentId: String, signature: String) {
        val checkout = pendingCheckout
        if (checkout != null) {
            _results.emit(PaymentOutcome.Success(checkout.order, checkout.planName, paymentId, signature))
        }
        pendingCheckout = null
    }

    suspend fun completeFailure(message: String, code: Int? = null, rawResponse: String? = null) {
        _results.emit(PaymentOutcome.Failure(pendingCheckout?.order, message, code, rawResponse))
        pendingCheckout = null
    }
}
