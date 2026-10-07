package com.freshsave.app.feature.favorites

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.freshsave.app.data.AppClock
import com.freshsave.app.data.model.AlertSettings
import com.freshsave.app.data.model.Shop
import com.freshsave.app.data.repository.DealRepository
import com.freshsave.app.data.repository.ShopRepository
import com.freshsave.app.data.fake.MockData
import com.freshsave.app.navigation.LocalSnackbarHostState
import com.freshsave.app.ui.appViewModel
import com.freshsave.app.ui.components.EmptyState
import com.freshsave.app.ui.components.FsTopBar
import com.freshsave.app.ui.components.RowDivider
import com.freshsave.app.ui.components.RowGroup
import com.freshsave.app.ui.components.Thumbnail
import com.freshsave.app.ui.components.ToggleRow
import com.freshsave.app.ui.theme.FreshSaveTheme
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class FavoriteShopItem(
    val shop: Shop,
    val following: Boolean,
    val liveDeals: Int,
    /** Where "jump to its live deals" goes; null when nothing is on sale. */
    val firstLiveDealId: String?,
)

data class FavoritesUiState(
    val loading: Boolean = true,
    val shops: List<FavoriteShopItem> = emptyList(),
    val alerts: AlertSettings = AlertSettings(),
)

class FavoritesViewModel(
    clock: AppClock,
    private val shopRepository: ShopRepository,
    dealRepository: DealRepository,
) : ViewModel() {
    private val minute = clock.now.map { it / 60_000 }.distinctUntilChanged()

    val state: StateFlow<FavoritesUiState> = combine(
        shopRepository.observeShops(),
        shopRepository.observeFavorites(),
        dealRepository.observeDeals(),
        shopRepository.observeAlertSettings(),
        minute,
    ) { shops, favorites, deals, alerts, _ ->
        val now = System.currentTimeMillis()
        val items = favorites.mapNotNull { (shopId, following) ->
            val shop = shops.firstOrNull { it.id == shopId } ?: return@mapNotNull null
            val live = deals.filter { it.shopId == shopId && it.isAvailable(now) }.sortedBy { it.pickupEndMillis }
            FavoriteShopItem(shop, following, live.size, live.firstOrNull()?.id)
        }
        FavoritesUiState(loading = false, shops = items, alerts = alerts)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), FavoritesUiState())

    fun setFollowing(shopId: String, following: Boolean) {
        viewModelScope.launch { shopRepository.setFollowing(shopId, following) }
    }

    fun updateAlerts(change: (AlertSettings) -> AlertSettings) {
        viewModelScope.launch { shopRepository.updateAlertSettings(change(state.value.alerts)) }
    }
}

@Composable
fun FavoritesRoute(onBack: () -> Unit, onDealClick: (String) -> Unit) {
    val viewModel = appViewModel { FavoritesViewModel(it.clock, it.shopRepository, it.dealRepository) }
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbar = LocalSnackbarHostState.current
    val scope = rememberCoroutineScope()
    FavoritesScreen(
        state = state,
        onBack = onBack,
        onShopClick = { item ->
            if (item.firstLiveDealId != null) onDealClick(item.firstLiveDealId)
            else scope.launch { snackbar.showSnackbar("${item.shop.name} has no deals right now — we'll alert you.") }
        },
        onFollowChange = viewModel::setFollowing,
        onAlertsChange = viewModel::updateAlerts,
    )
}

@Composable
fun FavoritesScreen(
    state: FavoritesUiState,
    onBack: () -> Unit,
    onShopClick: (FavoriteShopItem) -> Unit,
    onFollowChange: (String, Boolean) -> Unit,
    onAlertsChange: ((AlertSettings) -> AlertSettings) -> Unit,
) {
    val colors = FreshSaveTheme.colors
    Column(
        Modifier
            .fillMaxSize()
            .background(colors.background)
            .statusBarsPadding(),
    ) {
        FsTopBar("Favorites", onNavClick = onBack)
        LazyColumn(
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.navigationBarsPadding(),
        ) {
            item {
                Text(
                    "Get alerts when they post surplus",
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.textMuted,
                    modifier = Modifier.padding(bottom = 6.dp),
                )
            }
            if (!state.loading && state.shops.isEmpty()) {
                item {
                    EmptyState(
                        "No favorites yet",
                        "Tap the heart on any deal to save that shop here.",
                        icon = Icons.Outlined.FavoriteBorder,
                    )
                }
            }
            items(state.shops, key = { it.shop.id }) { item ->
                RowGroup(Modifier.animateItem()) {
                    ToggleRow(
                        title = item.shop.name,
                        subtitle = when (item.liveDeals) {
                            0 -> "No deals right now"
                            1 -> "1 deal live now"
                            else -> "${item.liveDeals} deals live now"
                        },
                        subtitleColor = if (item.liveDeals > 0) colors.green else colors.textMuted,
                        checked = item.following,
                        onCheckedChange = { onFollowChange(item.shop.id, it) },
                        leading = { Thumbnail(size = 44) },
                        onClick = { onShopClick(item) },
                    )
                }
            }
            item {
                Spacer(Modifier.height(12.dp))
                Text("Notification settings", style = MaterialTheme.typography.titleMedium, color = colors.textPrimary)
                Spacer(Modifier.height(10.dp))
                RowGroup(Modifier.padding(bottom = 4.dp)) {
                    ToggleRow(
                        "New deals from favorites",
                        state.alerts.newDealsFromFavorites,
                        { v -> onAlertsChange { it.copy(newDealsFromFavorites = v) } },
                    )
                    RowDivider()
                    ToggleRow(
                        "Deals ending soon nearby",
                        state.alerts.dealsEndingSoon,
                        { v -> onAlertsChange { it.copy(dealsEndingSoon = v) } },
                    )
                    RowDivider()
                    ToggleRow(
                        "Weekly impact recap",
                        state.alerts.weeklyImpactRecap,
                        { v -> onAlertsChange { it.copy(weeklyImpactRecap = v) } },
                    )
                }
            }
        }
    }
}

@Preview(widthDp = 380, heightDp = 800)
@Composable
private fun FavoritesPreview() {
    FreshSaveTheme {
        FavoritesScreen(
            FavoritesUiState(
                loading = false,
                shops = MockData.shops.take(3).mapIndexed { i, s -> FavoriteShopItem(s, i < 2, if (i == 0) 2 else 0, null) },
            ),
            {}, {}, { _, _ -> }, {},
        )
    }
}
