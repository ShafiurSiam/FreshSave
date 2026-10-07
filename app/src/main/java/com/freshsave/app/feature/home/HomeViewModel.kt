package com.freshsave.app.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.freshsave.app.data.AppClock
import com.freshsave.app.data.model.Deal
import com.freshsave.app.data.model.Shop
import com.freshsave.app.data.model.UserRole
import com.freshsave.app.data.repository.DealRepository
import com.freshsave.app.data.repository.NotificationRepository
import com.freshsave.app.data.repository.ShopRepository
import com.freshsave.app.data.repository.UserRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.util.Calendar

/** A price pin on the placeholder map. */
data class MapPin(val dealId: String, val price: Int, val x: Float, val y: Float, val endingSoonest: Boolean)

data class HomeUiState(
    val loading: Boolean = true,
    val greeting: String = "Hello",
    val firstName: String = "",
    val area: String = "Gulshan",
    val unreadNotifications: Int = 0,
    val endingSoon: List<Deal> = emptyList(),
    val pins: List<MapPin> = emptyList(),
)

val dhakaAreas = listOf("Gulshan", "Banani", "Dhanmondi", "Uttara", "Mohammadpur")

class HomeViewModel(
    clock: AppClock,
    dealRepository: DealRepository,
    shopRepository: ShopRepository,
    notificationRepository: NotificationRepository,
    userRepository: UserRepository,
) : ViewModel() {

    private val area = MutableStateFlow("Gulshan")

    // Re-check expiry every 30 seconds rather than every tick; the cards' own timers
    // still count down each second.
    private val coarseNow = clock.now.map { it / 30_000 * 30_000 }.distinctUntilChanged()

    val state: StateFlow<HomeUiState> = combine(
        dealRepository.observeDeals(),
        shopRepository.observeShops(),
        notificationRepository.observe(UserRole.SHOPPER),
        userRepository.observeCurrentUser(),
        combine(area, coarseNow, ::Pair),
    ) { deals, shops, notifications, user, (selectedArea, now) ->
        val live = deals.filter { it.isAvailable(now) && it.pickupStartMillis <= now + 3 * 60 * 60_000L }
            .sortedBy { it.pickupEndMillis }
        HomeUiState(
            loading = false,
            greeting = greetingFor(now),
            firstName = user.firstName,
            area = selectedArea,
            unreadNotifications = notifications.count { !it.read },
            endingSoon = live,
            pins = pinsFor(live, shops),
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUiState())

    fun selectArea(newArea: String) {
        area.value = newArea
    }

    private fun pinsFor(live: List<Deal>, shops: List<Shop>): List<MapPin> {
        // One pin per shop (its cheapest live deal), at most five so the mini-map stays readable.
        val soonest = live.firstOrNull()?.id
        return live.groupBy { it.shopId }
            .mapNotNull { (shopId, shopDeals) ->
                val shop = shops.firstOrNull { it.id == shopId } ?: return@mapNotNull null
                val deal = shopDeals.minBy { it.price }
                MapPin(deal.id, deal.price, shop.mapX, shop.mapY, endingSoonest = shopDeals.any { it.id == soonest })
            }
            .take(5)
    }

    private fun greetingFor(now: Long): String = when (Calendar.getInstance().apply { timeInMillis = now }.get(Calendar.HOUR_OF_DAY)) {
        in 5..11 -> "Good morning"
        in 12..16 -> "Good afternoon"
        else -> "Good evening"
    }
}
