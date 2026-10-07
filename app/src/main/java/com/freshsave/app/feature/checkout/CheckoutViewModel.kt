package com.freshsave.app.feature.checkout

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.freshsave.app.data.AppClock
import com.freshsave.app.data.fake.MockData
import com.freshsave.app.data.model.Deal
import com.freshsave.app.data.model.PaymentMethod
import com.freshsave.app.data.repository.DealRepository
import com.freshsave.app.data.repository.OrderRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class CheckoutUiState(
    val deal: Deal? = null,
    val quantity: Int = 1,
    val paymentMethod: PaymentMethod = PaymentMethod.BKASH,
    val paying: Boolean = false,
    val error: String? = null,
    /** Reserve now "locks" the item for 10 minutes while you pay. */
    val holdEndsAtMillis: Long = 0,
    val placedOrderId: String? = null,
) {
    val subtotal: Int get() = (deal?.price ?: 0) * quantity
    val serviceFee: Int get() = MockData.SERVICE_FEE
    val total: Int get() = subtotal + serviceFee
    val savings: Int get() = ((deal?.originalPrice ?: 0) - (deal?.price ?: 0)) * quantity
}

private data class CheckoutInputs(
    val method: PaymentMethod,
    val paying: Boolean,
    val error: String?,
    val placedOrderId: String?,
)

class CheckoutViewModel(
    private val dealId: String,
    private val quantity: Int,
    clock: AppClock,
    dealRepository: DealRepository,
    private val orderRepository: OrderRepository,
) : ViewModel() {

    private val holdEndsAt = clock.millis() + 10 * 60_000L
    private val inputs = MutableStateFlow(CheckoutInputs(PaymentMethod.BKASH, false, null, null))

    val state: StateFlow<CheckoutUiState> = combine(dealRepository.observeDeal(dealId), inputs) { deal, input ->
        CheckoutUiState(
            deal = deal,
            quantity = quantity,
            paymentMethod = input.method,
            paying = input.paying,
            error = input.error,
            holdEndsAtMillis = holdEndsAt,
            placedOrderId = input.placedOrderId,
        )
    }.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5_000),
        CheckoutUiState(quantity = quantity, holdEndsAtMillis = holdEndsAt),
    )

    fun selectMethod(method: PaymentMethod) = inputs.update { it.copy(method = method, error = null) }

    fun pay() {
        if (inputs.value.paying) return
        viewModelScope.launch {
            inputs.update { it.copy(paying = true, error = null) }
            // TODO: hand off to the real bKash / Nagad / card SDK here.
            orderRepository.placeOrder(dealId, quantity, inputs.value.method)
                .onSuccess { order -> inputs.update { it.copy(paying = false, placedOrderId = order.id) } }
                .onFailure { e -> inputs.update { it.copy(paying = false, error = e.message ?: "Payment failed.") } }
        }
    }

    fun onNavigated() = inputs.update { it.copy(placedOrderId = null) }
}
