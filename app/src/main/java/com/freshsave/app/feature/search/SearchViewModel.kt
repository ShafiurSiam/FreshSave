package com.freshsave.app.feature.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.freshsave.app.data.AppClock
import com.freshsave.app.data.model.Deal
import com.freshsave.app.data.model.FoodCategory
import com.freshsave.app.data.repository.DealRepository
import com.freshsave.app.util.isToday
import com.freshsave.app.util.isTomorrow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update

enum class PriceRange(val label: String, val min: Int, val max: Int) {
    UNDER_100("Under ৳100", 0, 99),
    FROM_100_TO_300("৳100–300", 100, 300),
    OVER_300("৳300+", 301, Int.MAX_VALUE),
}

enum class PickupTime(val label: String) { NOW("Now"), THIS_EVENING("This evening"), TOMORROW("Tomorrow") }

/** The user's filter choices. Kept separate from results so "Reset" is one line. */
data class SearchFilters(
    val query: String = "",
    val maxDistanceKm: Float = MAX_DISTANCE_KM,
    val price: PriceRange? = null,
    val categories: Set<FoodCategory> = emptySet(),
    val pickupTime: PickupTime? = null,
) {
    companion object {
        const val MAX_DISTANCE_KM = 5f
    }
}

data class SearchUiState(
    val filters: SearchFilters = SearchFilters(),
    val results: List<Deal> = emptyList(),
    val showingResults: Boolean = false,
)

class SearchViewModel(clock: AppClock, dealRepository: DealRepository) : ViewModel() {

    private val filters = MutableStateFlow(SearchFilters())
    private val showingResults = MutableStateFlow(false)
    private val minuteNow = clock.now.map { it / 60_000 * 60_000 }.distinctUntilChanged()

    val state: StateFlow<SearchUiState> = combine(
        dealRepository.observeDeals(), filters, showingResults, minuteNow,
    ) { deals, f, showing, now ->
        SearchUiState(filters = f, results = applyFilters(deals, f, now), showingResults = showing)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SearchUiState())

    fun onQueryChange(query: String) = filters.update { it.copy(query = query) }
    fun onDistanceChange(km: Float) = filters.update { it.copy(maxDistanceKm = km) }
    fun onPriceSelected(range: PriceRange) = filters.update { it.copy(price = if (it.price == range) null else range) }
    fun onPickupTimeSelected(time: PickupTime) = filters.update { it.copy(pickupTime = if (it.pickupTime == time) null else time) }
    fun onCategoryToggled(category: FoodCategory) = filters.update {
        it.copy(categories = if (category in it.categories) it.categories - category else it.categories + category)
    }

    fun reset() {
        filters.value = SearchFilters()
        showingResults.value = false
    }

    fun showResults() {
        showingResults.value = true
    }

    fun editFilters() {
        showingResults.value = false
    }

    companion object {
        /** Pure function so it is easy to read (and to unit-test later). */
        fun applyFilters(deals: List<Deal>, f: SearchFilters, now: Long): List<Deal> {
            val query = f.query.trim().lowercase()
            return deals.filter { deal ->
                deal.isAvailable(now) &&
                    (query.isEmpty() || listOf(deal.title, deal.shopName, deal.description, deal.category.label, deal.area)
                        .any { it.lowercase().contains(query) }) &&
                    (f.maxDistanceKm >= SearchFilters.MAX_DISTANCE_KM || deal.distanceKm <= f.maxDistanceKm) &&
                    (f.price == null || deal.price in f.price.min..f.price.max) &&
                    (f.categories.isEmpty() || deal.category in f.categories) &&
                    when (f.pickupTime) {
                        null -> true
                        PickupTime.NOW -> deal.pickupStartMillis <= now
                        PickupTime.THIS_EVENING -> isToday(deal.pickupEndMillis, now)
                        PickupTime.TOMORROW -> isTomorrow(deal.pickupStartMillis, now)
                    }
            }.sortedBy { it.pickupEndMillis }
        }
    }
}
