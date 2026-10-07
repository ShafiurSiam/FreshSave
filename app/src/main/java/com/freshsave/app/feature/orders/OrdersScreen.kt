package com.freshsave.app.feature.orders

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.ShoppingBag
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.freshsave.app.data.AppClock
import com.freshsave.app.data.fake.MockData
import com.freshsave.app.data.model.Order
import com.freshsave.app.data.model.OrderStatus
import com.freshsave.app.data.repository.OrderRepository
import com.freshsave.app.feature.confirmation.qrPayloadFor
import com.freshsave.app.ui.appViewModel
import com.freshsave.app.ui.components.EmptyState
import com.freshsave.app.ui.components.FsCard
import com.freshsave.app.ui.components.LocalNow
import com.freshsave.app.ui.components.PickupCodeCard
import com.freshsave.app.ui.components.ScreenTitle
import com.freshsave.app.ui.components.SegmentedTabs
import com.freshsave.app.ui.components.StatusPill
import com.freshsave.app.ui.components.Thumbnail
import com.freshsave.app.ui.components.TimerBadge
import com.freshsave.app.ui.components.TimerStyle
import com.freshsave.app.ui.components.TonalButton
import com.freshsave.app.ui.theme.FreshSaveTheme
import com.freshsave.app.util.formatClockTime
import com.freshsave.app.util.formatDayWord
import com.freshsave.app.util.formatShortDate
import com.freshsave.app.util.formatTaka
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

data class OrdersUiState(
    val loading: Boolean = true,
    val active: List<Order> = emptyList(),
    val past: List<Order> = emptyList(),
)

class OrdersViewModel(clock: AppClock, orderRepository: OrderRepository) : ViewModel() {
    // Orders move from Active to Past when their window closes, so re-sort every 10 s.
    private val tick = clock.now.map { it / 10_000 }.distinctUntilChanged()

    val state: StateFlow<OrdersUiState> = combine(orderRepository.observeMyOrders(), tick) { orders, _ ->
        val now = System.currentTimeMillis()
        val (active, past) = orders.partition { it.effectiveStatus(now) == OrderStatus.ACTIVE }
        OrdersUiState(
            loading = false,
            active = active.sortedBy { it.pickupStartMillis },
            past = past.sortedByDescending { it.collectedAtMillis ?: it.pickupEndMillis },
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), OrdersUiState())
}

@Composable
fun OrdersRoute(onOrderClick: (String) -> Unit, onRate: (String) -> Unit, onBrowse: () -> Unit) {
    val viewModel = appViewModel { OrdersViewModel(it.clock, it.orderRepository) }
    val state by viewModel.state.collectAsStateWithLifecycle()
    OrdersScreen(state, onOrderClick, onRate, onBrowse)
}

@Composable
fun OrdersScreen(
    state: OrdersUiState,
    onOrderClick: (String) -> Unit,
    onRate: (String) -> Unit,
    onBrowse: () -> Unit,
) {
    val colors = FreshSaveTheme.colors
    // rememberSaveable keeps the chosen tab across rotation and tab switches.
    var tab by rememberSaveable { mutableIntStateOf(0) }
    Column(
        Modifier
            .fillMaxSize()
            .background(colors.background)
            .statusBarsPadding(),
    ) {
        Column(Modifier.padding(horizontal = 20.dp, vertical = 12.dp)) {
            ScreenTitle("My orders")
            Spacer(Modifier.height(16.dp))
            SegmentedTabs(
                options = listOf(
                    "Active" + if (state.active.isNotEmpty()) " (${state.active.size})" else "",
                    "Past",
                ),
                selectedIndex = tab,
                onSelect = { tab = it },
            )
        }
        val list = if (tab == 0) state.active else state.past
        LazyColumn(
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 4.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            if (!state.loading && list.isEmpty()) {
                item {
                    if (tab == 0) {
                        EmptyState(
                            "No active reservations",
                            "Reserve a deal and your pickup code will appear here.",
                            icon = Icons.Outlined.ShoppingBag,
                            actionLabel = "Browse deals",
                            onAction = onBrowse,
                        )
                    } else {
                        EmptyState("No past orders yet", "Collected rescues will show up here.", icon = Icons.Outlined.History)
                    }
                }
            }
            items(list, key = { it.id }) { order ->
                if (tab == 0) {
                    ActiveOrderCard(order, onClick = { onOrderClick(order.id) }, modifier = Modifier.animateItem())
                } else {
                    PastOrderCard(order, onClick = { onOrderClick(order.id) }, onRate = { onRate(order.id) }, modifier = Modifier.animateItem())
                }
            }
        }
    }
}

@Composable
private fun ActiveOrderCard(order: Order, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val colors = FreshSaveTheme.colors
    val now = LocalNow.current
    val ready = order.isReadyToCollect(now)
    FsCard(modifier = modifier, onClick = onClick) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (ready) {
                StatusPill("Ready to collect", showDot = true)
            } else {
                StatusPill("Reserved", background = colors.surfaceMuted, content = colors.textPrimary)
            }
            Spacer(Modifier.weight(1f))
            if (ready) {
                TimerBadge(order.pickupEndMillis, style = TimerStyle.Tint)
            } else {
                Text(
                    "Pickup ${formatClockTime(order.pickupStartMillis)}" +
                        formatDayWord(order.pickupStartMillis, now).let { if (it == "today") "" else " $it" },
                    style = MaterialTheme.typography.labelMedium,
                    color = colors.textMuted,
                )
            }
        }
        Spacer(Modifier.height(14.dp))
        OrderSummaryRow(order, showPrice = !ready)
        if (ready) {
            Spacer(Modifier.height(14.dp))
            PickupCodeCard(order.pickupCode, qrPayloadFor(order))
        }
    }
}

@Composable
private fun PastOrderCard(order: Order, onClick: () -> Unit, onRate: () -> Unit, modifier: Modifier = Modifier) {
    val colors = FreshSaveTheme.colors
    val now = LocalNow.current
    val status = order.effectiveStatus(now)
    FsCard(modifier = modifier, onClick = onClick) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (status == OrderStatus.COLLECTED) {
                StatusPill("Collected · ${formatShortDate(order.collectedAtMillis ?: order.pickupEndMillis)}")
            } else {
                StatusPill("Missed", background = colors.dangerTint, content = colors.danger)
            }
            Spacer(Modifier.weight(1f))
            Text("Saved ${formatTaka(order.savings)}", style = MaterialTheme.typography.labelMedium, color = colors.green)
        }
        Spacer(Modifier.height(14.dp))
        OrderSummaryRow(order, showPrice = true)
        if (status == OrderStatus.COLLECTED) {
            Spacer(Modifier.height(12.dp))
            if (order.reviewed) {
                TonalButton("Rated — thank you", {}, leadingIcon = Icons.Filled.Check, content = colors.green, container = colors.greenTint)
            } else {
                TonalButton("Rate this pickup", onRate, leadingIcon = Icons.Outlined.StarOutline, content = colors.orange, container = colors.orangeTint)
            }
        }
    }
}

@Composable
private fun OrderSummaryRow(order: Order, showPrice: Boolean) {
    val colors = FreshSaveTheme.colors
    Row(verticalAlignment = Alignment.CenterVertically) {
        Thumbnail(size = 52)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                (if (order.quantity > 1) "${order.quantity}× " else "") + order.itemTitle,
                style = MaterialTheme.typography.titleMedium,
                color = colors.textPrimary,
            )
            Text(
                "${order.shopName}, ${order.area}" + if (showPrice) " · ${formatTaka(order.subtotal)}" else "",
                style = MaterialTheme.typography.bodySmall,
                color = colors.textMuted,
            )
        }
    }
}

@Preview(widthDp = 380, heightDp = 800)
@Composable
private fun OrdersPreview() {
    val orders = MockData.orders(System.currentTimeMillis()).filter { it.customerId == MockData.ME_ID }
    FreshSaveTheme {
        OrdersScreen(
            OrdersUiState(false, orders.filter { it.status == OrderStatus.ACTIVE }, orders.filter { it.status != OrderStatus.ACTIVE }),
            {}, {}, {},
        )
    }
}
