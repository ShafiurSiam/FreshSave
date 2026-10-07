package com.freshsave.app.feature.business.listings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.Sell
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.freshsave.app.data.AppClock
import com.freshsave.app.data.fake.MockData
import com.freshsave.app.data.model.Deal
import com.freshsave.app.data.model.DealStatus
import com.freshsave.app.data.repository.BusinessRepository
import com.freshsave.app.data.repository.DealRepository
import com.freshsave.app.navigation.LocalSnackbarHostState
import com.freshsave.app.ui.appViewModel
import com.freshsave.app.ui.components.EmptyState
import com.freshsave.app.ui.components.FsCard
import com.freshsave.app.ui.components.LocalNow
import com.freshsave.app.ui.components.ScreenTitle
import com.freshsave.app.ui.components.StatusPill
import com.freshsave.app.ui.components.Thumbnail
import com.freshsave.app.ui.components.TimerBadge
import com.freshsave.app.ui.components.TimerStyle
import com.freshsave.app.ui.components.TonalButton
import com.freshsave.app.ui.theme.FreshSaveTheme
import com.freshsave.app.util.formatClockTime
import com.freshsave.app.util.formatTaka
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Listings closing within this window offer "Extend window" instead of "Expire now". */
private const val ENDING_SOON_MS = 30 * 60_000L

data class ListingsUiState(val loading: Boolean = true, val listings: List<Deal> = emptyList(), val liveCount: Int = 0)

class ManageListingsViewModel(
    clock: AppClock,
    businessRepository: BusinessRepository,
    private val dealRepository: DealRepository,
) : ViewModel() {
    private val tick = clock.now.map { it / 15_000 }.distinctUntilChanged()

    val state: StateFlow<ListingsUiState> = combine(
        dealRepository.observeShopDeals(businessRepository.currentShopId), tick,
    ) { deals, _ ->
        val now = System.currentTimeMillis()
        // Live first (soonest ending on top), then sold out, then expired.
        val sorted = deals.sortedWith(compareBy<Deal>({ it.status(now).ordinal }, { it.pickupEndMillis }))
        ListingsUiState(false, sorted, deals.count { it.status(now) == DealStatus.LIVE })
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ListingsUiState())

    fun expireNow(id: String) = viewModelScope.launch { dealRepository.expireNow(id) }
    fun extend(id: String) = viewModelScope.launch { dealRepository.extendWindow(id, 30) }
    fun relist(id: String) = viewModelScope.launch { dealRepository.relist(id) }
}

@Composable
fun ManageListingsRoute(onEdit: (String) -> Unit, onCreate: () -> Unit) {
    val viewModel = appViewModel { ManageListingsViewModel(it.clock, it.businessRepository, it.dealRepository) }
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbar = LocalSnackbarHostState.current
    val scope = rememberCoroutineScope()
    fun toast(message: String) = scope.launch {
        snackbar.currentSnackbarData?.dismiss()
        snackbar.showSnackbar(message)
    }
    ManageListingsScreen(
        state = state,
        onEdit = onEdit,
        onCreate = onCreate,
        onExpire = { deal ->
            viewModel.expireNow(deal.id)
            toast("${deal.title} taken down")
        },
        onExtend = { deal ->
            viewModel.extend(deal.id)
            toast("${deal.title} extended by 30 min")
        },
        onRelist = { deal ->
            viewModel.relist(deal.id)
            toast("${deal.title} is live again for 2 hours")
        },
    )
}

@Composable
fun ManageListingsScreen(
    state: ListingsUiState,
    onEdit: (String) -> Unit,
    onCreate: () -> Unit,
    onExpire: (Deal) -> Unit,
    onExtend: (Deal) -> Unit,
    onRelist: (Deal) -> Unit,
) {
    val colors = FreshSaveTheme.colors
    Column(
        Modifier
            .fillMaxSize()
            .background(colors.background)
            .statusBarsPadding(),
    ) {
        ScreenTitle(
            "Active listings",
            Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
        ) {
            StatusPill("${state.liveCount} live")
            Spacer(Modifier.width(8.dp))
            TonalButton("New", onCreate, leadingIcon = Icons.Filled.Add, container = colors.green, content = colors.onGreen)
        }
        LazyColumn(
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (!state.loading && state.listings.isEmpty()) {
                item {
                    EmptyState(
                        "No listings yet",
                        "Post today's surplus and it will show up for shoppers nearby.",
                        icon = Icons.Outlined.Sell,
                        actionLabel = "Post surplus listing",
                        onAction = onCreate,
                    )
                }
            }
            items(state.listings, key = { it.id }) { deal ->
                ListingCard(deal, onEdit, onExpire, onExtend, onRelist, Modifier.animateItem())
            }
        }
    }
}

@Composable
private fun ListingCard(
    deal: Deal,
    onEdit: (String) -> Unit,
    onExpire: (Deal) -> Unit,
    onExtend: (Deal) -> Unit,
    onRelist: (Deal) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = FreshSaveTheme.colors
    val now = LocalNow.current
    val status = deal.status(now)
    val endingSoon = status == DealStatus.LIVE && deal.pickupEndMillis - now <= ENDING_SOON_MS
    FsCard(modifier = modifier.fillMaxWidth()) {
        Row(
            Modifier.alpha(if (status == DealStatus.LIVE) 1f else 0.6f),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Thumbnail(deal.imageUrl, size = 52)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        deal.title,
                        style = MaterialTheme.typography.titleMedium,
                        color = colors.textPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false),
                    )
                    Spacer(Modifier.width(8.dp))
                    when {
                        endingSoon -> TimerBadge(deal.pickupEndMillis, style = TimerStyle.Tint)
                        status == DealStatus.LIVE -> StatusPill("Live", showDot = true)
                        status == DealStatus.SOLD_OUT -> StatusPill("Sold out", background = colors.surfaceMuted, content = colors.textMuted)
                        else -> StatusPill("Expired", background = colors.surfaceMuted, content = colors.textMuted)
                    }
                }
                Text(
                    when (status) {
                        DealStatus.SOLD_OUT ->
                            "${formatTaka(deal.price)} · 0 of ${deal.quantityTotal} left · earned ${formatTaka(deal.price * deal.quantitySold)}"
                        DealStatus.EXPIRED -> "${formatTaka(deal.price)} · ${deal.quantityLeft} of ${deal.quantityTotal} unsold"
                        DealStatus.LIVE ->
                            "${formatTaka(deal.price)} · ${deal.quantityLeft} of ${deal.quantityTotal} left · ends ${formatClockTime(deal.pickupEndMillis)}"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.textMuted,
                )
            }
        }
        if (status == DealStatus.LIVE) {
            Spacer(Modifier.height(10.dp))
            // Bar fills up as items sell.
            LinearProgressIndicator(
                progress = { deal.quantitySold / deal.quantityTotal.coerceAtLeast(1).toFloat() },
                color = if (endingSoon) colors.orange else colors.green,
                trackColor = colors.surfaceMuted,
                strokeCap = StrokeCap.Round,
                gapSize = 0.dp,
                drawStopIndicator = {},
                modifier = Modifier.fillMaxWidth().height(5.dp),
            )
        }
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            when {
                status != DealStatus.LIVE ->
                    TonalButton("Relist", { onRelist(deal) }, Modifier.weight(1f))
                endingSoon -> {
                    TonalButton("Edit", { onEdit(deal.id) }, Modifier.weight(1f))
                    TonalButton("Extend window", { onExtend(deal) }, Modifier.weight(1f), container = colors.greenTint, content = colors.green)
                }
                else -> {
                    TonalButton("Edit", { onEdit(deal.id) }, Modifier.weight(1f))
                    TonalButton("Expire now", { onExpire(deal) }, Modifier.weight(1f), container = colors.dangerTint, content = colors.danger)
                }
            }
        }
    }
}

@Preview(widthDp = 380, heightDp = 800)
@Composable
private fun ListingsPreview() {
    val deals = MockData.deals(System.currentTimeMillis()).filter { it.shopId == MockData.BUSINESS_SHOP_ID }
    FreshSaveTheme { ManageListingsScreen(ListingsUiState(false, deals, 2), {}, {}, {}, {}, {}) }
}
