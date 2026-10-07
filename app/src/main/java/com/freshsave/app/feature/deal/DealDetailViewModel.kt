package com.freshsave.app.feature.deal

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.freshsave.app.data.model.Deal
import com.freshsave.app.data.repository.DealRepository
import com.freshsave.app.data.repository.ShopRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class DealDetailUiState(
    val loading: Boolean = true,
    val deal: Deal? = null,
    val isFavorite: Boolean = false,
    val quantity: Int = 1,
) {
    val total: Int get() = (deal?.price ?: 0) * quantity
}

class DealDetailViewModel(
    private val dealId: String,
    dealRepository: DealRepository,
    private val shopRepository: ShopRepository,
) : ViewModel() {

    private val quantity = MutableStateFlow(1)

    val state: StateFlow<DealDetailUiState> = combine(
        dealRepository.observeDeal(dealId),
        shopRepository.observeFavorites(),
        quantity,
    ) { deal, favorites, qty ->
        // Never let the stepper exceed what's actually left (stock can drop while you look).
        val maxQty = (deal?.quantityLeft ?: 1).coerceAtLeast(1)
        DealDetailUiState(
            loading = false,
            deal = deal,
            isFavorite = deal != null && deal.shopId in favorites,
            quantity = qty.coerceIn(1, maxQty),
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DealDetailUiState())

    fun increment() {
        val left = state.value.deal?.quantityLeft ?: return
        quantity.update { (it + 1).coerceAtMost(left.coerceAtLeast(1)) }
    }

    fun decrement() = quantity.update { (it - 1).coerceAtLeast(1) }

    /** Returns the new favourite state so the screen can show the right snackbar. */
    fun toggleFavorite(): Boolean {
        val deal = state.value.deal ?: return false
        val newValue = !state.value.isFavorite
        viewModelScope.launch { shopRepository.setFavorite(deal.shopId, newValue) }
        return newValue
    }
}
