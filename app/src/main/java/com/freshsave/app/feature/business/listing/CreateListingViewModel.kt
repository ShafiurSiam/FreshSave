package com.freshsave.app.feature.business.listing

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.freshsave.app.data.AppClock
import com.freshsave.app.data.model.FoodCategory
import com.freshsave.app.data.model.ListingDraft
import com.freshsave.app.data.repository.BusinessRepository
import com.freshsave.app.data.repository.DealRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/** When pickup starts, relative to publishing. */
enum class StartOption(val label: String, val offsetMinutes: Int) { NOW("Now", 0), IN_30("In 30 min", 30), IN_60("In 1 hour", 60) }

/** How long the pickup window stays open. After that the listing auto-expires. */
enum class WindowOption(val label: String, val minutes: Int) { H1("1 hour", 60), H2("2 hours", 120), H3("3 hours", 180), H4("4 hours", 240) }

data class ListingForm(
    val imageUrl: String? = null,
    val title: String = "Surprise Bakery Bag",
    val category: FoodCategory = FoodCategory.BAKERY,
    val originalPrice: String = "250",
    val quantity: String = "8",
    val discountPercent: Int = 70,
    val start: StartOption = StartOption.NOW,
    val window: WindowOption = WindowOption.H2,
    val description: String = "",
) {
    val originalPriceValue: Int get() = originalPrice.toIntOrNull() ?: 0
    val quantityValue: Int get() = quantity.toIntOrNull() ?: 0
    /** Sale price, recalculated whenever the price or discount changes. */
    val salePrice: Int get() = (originalPriceValue * (100 - discountPercent) / 100.0).roundToInt()
}

data class CreateListingUiState(
    val editing: Boolean = false,
    val form: ListingForm = ListingForm(),
    val errors: Map<String, String> = emptyMap(),
    val saving: Boolean = false,
    val saved: Boolean = false,
)

class CreateListingViewModel(
    private val editDealId: String?,
    private val clock: AppClock,
    private val dealRepository: DealRepository,
    private val businessRepository: BusinessRepository,
) : ViewModel() {
    private val _state = MutableStateFlow(CreateListingUiState(editing = editDealId != null))
    val state: StateFlow<CreateListingUiState> = _state.asStateFlow()

    init {
        // In edit mode, pre-fill the form from the existing listing.
        if (editDealId != null) {
            viewModelScope.launch {
                val deal = dealRepository.observeDeal(editDealId).first() ?: return@launch
                val remainingMinutes = ((deal.pickupEndMillis - clock.millis()) / 60_000).toInt()
                _state.update {
                    it.copy(
                        form = ListingForm(
                            imageUrl = deal.imageUrl,
                            title = deal.title,
                            category = deal.category,
                            originalPrice = deal.originalPrice.toString(),
                            quantity = deal.quantityTotal.toString(),
                            discountPercent = deal.discountPercent,
                            start = StartOption.NOW,
                            window = WindowOption.entries.firstOrNull { w -> w.minutes >= remainingMinutes } ?: WindowOption.H4,
                            description = deal.description,
                        ),
                    )
                }
            }
        }
    }

    fun update(change: (ListingForm) -> ListingForm) = _state.update { it.copy(form = change(it.form), errors = emptyMap()) }

    fun pickupWindow(form: ListingForm = _state.value.form, now: Long = clock.millis()): Pair<Long, Long> {
        val start = now + form.start.offsetMinutes * 60_000L
        return start to start + form.window.minutes * 60_000L
    }

    fun publish() {
        val form = _state.value.form
        val errors = buildMap {
            if (form.title.isBlank()) put("title", "Give the item a name")
            if (form.originalPriceValue <= 0) put("price", "Enter the original price")
            if (form.quantityValue <= 0) put("quantity", "At least 1")
        }
        if (errors.isNotEmpty()) {
            _state.update { it.copy(errors = errors) }
            return
        }
        viewModelScope.launch {
            _state.update { it.copy(saving = true) }
            val (start, end) = pickupWindow(form)
            val draft = ListingDraft(
                title = form.title.trim(),
                category = form.category,
                originalPrice = form.originalPriceValue,
                quantity = form.quantityValue,
                discountPercent = form.discountPercent,
                pickupStartMillis = start,
                pickupEndMillis = end,
                description = form.description.trim().ifBlank { "Fresh ${form.category.label.lowercase()} surplus from today." },
                imageUrl = form.imageUrl,
            )
            if (editDealId != null) dealRepository.updateDeal(editDealId, draft)
            else dealRepository.createDeal(businessRepository.currentShopId, draft)
            _state.update { it.copy(saving = false, saved = true) }
        }
    }
}
