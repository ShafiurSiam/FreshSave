package com.freshsave.app.feature.business.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.ShoppingBag
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.freshsave.app.R
import com.freshsave.app.data.AppClock
import com.freshsave.app.data.fake.MockData
import com.freshsave.app.data.model.DealStatus
import com.freshsave.app.data.model.OrderStatus
import com.freshsave.app.data.model.UserRole
import com.freshsave.app.data.repository.BusinessRepository
import com.freshsave.app.data.repository.DealRepository
import com.freshsave.app.data.repository.NotificationRepository
import com.freshsave.app.data.repository.OrderRepository
import com.freshsave.app.data.repository.ReviewRepository
import com.freshsave.app.ui.appViewModel
import com.freshsave.app.ui.components.PrimaryButton
import com.freshsave.app.ui.components.StatTile
import com.freshsave.app.ui.components.Thumbnail
import com.freshsave.app.ui.theme.FreshSaveTheme
import com.freshsave.app.util.formatShortDate
import com.freshsave.app.util.formatTaka
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlin.math.roundToInt

data class DashboardUiState(
    val shopName: String = "",
    val branchName: String = "",
    val dateLabel: String = "",
    val unread: Int = 0,
    val revenueToday: Int = 0,
    val itemsListed: Int = 0,
    val itemsSold: Int = 0,
    val itemsExpiring: Int = 0,
    val kgThisWeek: Int = 0,
    val co2ThisWeek: Int = 0,
    val pickupsWaiting: Int = 0,
    val rating: Double = 0.0,
)

class DashboardViewModel(
    clock: AppClock,
    businessRepository: BusinessRepository,
    dealRepository: DealRepository,
    orderRepository: OrderRepository,
    notificationRepository: NotificationRepository,
    reviewRepository: ReviewRepository,
) : ViewModel() {
    private val shopId = businessRepository.currentShopId
    private val sessionStart = clock.sessionStartMillis
    private val minute = clock.now.map { it / 60_000 * 60_000 }.distinctUntilChanged()

    val state: StateFlow<DashboardUiState> = combine(
        combine(businessRepository.observeProfile(), reviewRepository.observeRatingSummary(shopId), ::Pair),
        dealRepository.observeShopDeals(shopId),
        orderRepository.observeShopOrders(shopId),
        notificationRepository.observe(UserRole.BUSINESS),
        minute,
    ) { (profile, rating), deals, orders, notifications, now ->
        val newOrders = orders.filter { it.createdAtMillis >= sessionStart }
        val newItems = newOrders.sumOf { it.quantity }
        val liveDeals = deals.filter { it.status(now) == DealStatus.LIVE }
        DashboardUiState(
            shopName = profile.shop.name,
            branchName = "${profile.selectedBranch?.name ?: ""} branch",
            dateLabel = "Today · ${formatShortDate(now)}",
            unread = notifications.count { !it.read },
            revenueToday = MockData.BASE_REVENUE_TODAY + newOrders.sumOf { it.subtotal },
            itemsListed = deals.sumOf { it.quantityTotal },
            itemsSold = deals.sumOf { it.quantitySold },
            // Stock still on the shelf whose window closes within 90 minutes.
            itemsExpiring = liveDeals.filter { it.pickupEndMillis - now <= 90 * 60_000L }.sumOf { it.quantityLeft },
            kgThisWeek = 11 + (newItems * 0.8).roundToInt(),
            co2ThisWeek = 28 + newItems * 2,
            pickupsWaiting = orders.count { it.effectiveStatus(now) == OrderStatus.ACTIVE },
            rating = rating.average,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DashboardUiState())
}

@Composable
fun DashboardRoute(
    onNotifications: () -> Unit,
    onPostListing: () -> Unit,
    onOpenQueue: () -> Unit,
    onOpenRatings: () -> Unit,
) {
    val viewModel = appViewModel {
        DashboardViewModel(it.clock, it.businessRepository, it.dealRepository, it.orderRepository, it.notificationRepository, it.reviewRepository)
    }
    val state by viewModel.state.collectAsStateWithLifecycle()
    DashboardScreen(state, onNotifications, onPostListing, onOpenQueue, onOpenRatings)
}

@Composable
fun DashboardScreen(
    state: DashboardUiState,
    onNotifications: () -> Unit,
    onPostListing: () -> Unit,
    onOpenQueue: () -> Unit,
    onOpenRatings: () -> Unit,
) {
    val colors = FreshSaveTheme.colors
    Column(
        Modifier
            .fillMaxSize()
            .background(colors.background)
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Thumbnail(size = 48)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(state.shopName, style = MaterialTheme.typography.titleLarge, color = colors.textPrimary)
                Text(state.branchName, style = MaterialTheme.typography.bodySmall, color = colors.textMuted)
            }
            Box(
                Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(colors.greenTint)
                    .clickable(onClick = onNotifications),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Outlined.Notifications, "Notifications", tint = colors.green)
                if (state.unread > 0) {
                    Box(Modifier.align(Alignment.TopEnd).padding(10.dp).size(9.dp).background(colors.orange, CircleShape))
                }
            }
        }
        Spacer(Modifier.height(16.dp))
        Text(state.dateLabel, style = MaterialTheme.typography.bodyMedium, color = colors.textMuted)
        Spacer(Modifier.height(10.dp))

        // Revenue card.
        Column(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(Brush.linearGradient(listOf(colors.green, colors.greenDark)))
                .padding(20.dp),
        ) {
            Text("Revenue recovered today", style = MaterialTheme.typography.bodyMedium, color = Color.White.copy(alpha = 0.85f))
            Text(
                formatTaka(state.revenueToday),
                style = MaterialTheme.typography.displaySmall.copy(fontSize = 38.sp),
                color = Color.White,
                maxLines = 1,
            )
            Text(
                "from ${state.itemsSold} items sold · +22% vs yesterday",
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.85f),
            )
        }
        Spacer(Modifier.height(12.dp))
        Row(Modifier.height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            StatTile("${state.itemsListed}", "items\nlisted", Modifier.weight(1f).fillMaxHeight(), valueColor = colors.textPrimary)
            StatTile("${state.itemsSold}", "sold", Modifier.weight(1f).fillMaxHeight())
            StatTile("${state.itemsExpiring}", "expiring", Modifier.weight(1f).fillMaxHeight(), valueColor = colors.orange)
        }
        Spacer(Modifier.height(12.dp))
        Row(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(colors.darkCard)
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(painterResource(R.drawable.ic_leaf), null, tint = Color(0xFF7BD3A0), modifier = Modifier.size(24.dp))
            Spacer(Modifier.width(14.dp))
            Column {
                Text("${state.kgThisWeek} kg food rescued this week", style = MaterialTheme.typography.titleSmall, color = Color.White)
                Text("≈ ${state.co2ThisWeek} kg CO₂ avoided", style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.7f))
            }
        }
        Spacer(Modifier.height(16.dp))
        PrimaryButton(
            text = "Post surplus listing",
            onClick = onPostListing,
            containerColor = colors.orange,
            leadingIcon = Icons.Filled.Add,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(16.dp))
        Row(Modifier.height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Shortcut(Icons.Outlined.ShoppingBag, "${state.pickupsWaiting} to collect", onOpenQueue, Modifier.weight(1f).fillMaxHeight())
            Shortcut(Icons.Filled.Star, "${state.rating} rating", onOpenRatings, Modifier.weight(1f).fillMaxHeight())
        }
        Spacer(Modifier.height(20.dp))
    }
}

@Composable
private fun Shortcut(icon: ImageVector, label: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val colors = FreshSaveTheme.colors
    Row(
        modifier
            .clip(RoundedCornerShape(14.dp))
            .background(colors.card)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, null, tint = colors.green, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(8.dp))
        Text(label, style = MaterialTheme.typography.titleSmall, color = colors.textPrimary, modifier = Modifier.weight(1f))
        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = colors.textMuted, modifier = Modifier.size(18.dp))
    }
}

@Preview(widthDp = 380, heightDp = 820)
@Composable
private fun DashboardPreview() {
    FreshSaveTheme {
        DashboardScreen(
            DashboardUiState("Tasty Treat", "Banani branch", "Today · Wed 23 Jul", 1, 3450, 18, 14, 4, 11, 28, 3, 4.8),
            {}, {}, {}, {},
        )
    }
}
