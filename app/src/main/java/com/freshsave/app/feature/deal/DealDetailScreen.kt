package com.freshsave.app.feature.deal

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.freshsave.app.data.fake.MockData
import com.freshsave.app.data.model.Deal
import com.freshsave.app.data.model.DealStatus
import com.freshsave.app.navigation.LocalSnackbarHostState
import com.freshsave.app.ui.appViewModel
import com.freshsave.app.ui.components.CircleIconButton
import com.freshsave.app.ui.components.DealImage
import com.freshsave.app.ui.components.DiscountBadge
import com.freshsave.app.ui.components.EmptyState
import com.freshsave.app.ui.components.LocalNow
import com.freshsave.app.ui.components.PrimaryButton
import com.freshsave.app.ui.theme.FreshSaveTheme
import com.freshsave.app.ui.theme.StatusBarIcons
import com.freshsave.app.util.formatDayWord
import com.freshsave.app.util.formatDistance
import com.freshsave.app.util.formatTaka
import com.freshsave.app.util.formatTimeLeft
import com.freshsave.app.util.formatTimeRange
import kotlinx.coroutines.launch

@Composable
fun DealDetailRoute(
    dealId: String,
    onBack: () -> Unit,
    onReserve: (dealId: String, quantity: Int) -> Unit,
    onOpenFavorites: () -> Unit,
) {
    // key = dealId gives each deal its own ViewModel if several are on the back stack.
    val viewModel = appViewModel(key = "deal-$dealId") { DealDetailViewModel(dealId, it.dealRepository, it.shopRepository) }
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbar = LocalSnackbarHostState.current
    val scope = rememberCoroutineScope()

    DealDetailScreen(
        state = state,
        onBack = onBack,
        onToggleFavorite = {
            val saved = viewModel.toggleFavorite()
            val shop = state.deal?.shopName ?: "shop"
            scope.launch {
                snackbar.currentSnackbarData?.dismiss()
                val result = snackbar.showSnackbar(
                    message = if (saved) "Saved $shop to favorites" else "Removed $shop from favorites",
                    actionLabel = if (saved) "View" else null,
                    duration = SnackbarDuration.Short,
                )
                if (result == SnackbarResult.ActionPerformed) onOpenFavorites()
            }
        },
        onIncrement = viewModel::increment,
        onDecrement = viewModel::decrement,
        onReserve = { state.deal?.let { onReserve(it.id, state.quantity) } },
    )
}

@Composable
fun DealDetailScreen(
    state: DealDetailUiState,
    onBack: () -> Unit,
    onToggleFavorite: () -> Unit,
    onIncrement: () -> Unit,
    onDecrement: () -> Unit,
    onReserve: () -> Unit,
) {
    val colors = FreshSaveTheme.colors
    val deal = state.deal
    if (deal == null) {
        Box(Modifier.fillMaxSize().background(colors.background).statusBarsPadding(), contentAlignment = Alignment.Center) {
            if (state.loading) CircularProgressIndicator(color = colors.green)
            else EmptyState("Deal not found", "This listing may have been removed.", actionLabel = "Go back", onAction = onBack)
        }
        return
    }
    StatusBarIcons(lightIcons = deal.imageUrl != null)
    val now = LocalNow.current
    val status = deal.status(now)

    Column(Modifier.fillMaxSize().background(colors.background)) {
        Column(
            Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState()),
        ) {
            DealImage(
                imageUrl = deal.imageUrl,
                label = "hero photo · ${deal.imageLabel.removeSuffix(" photo")}",
                modifier = Modifier
                    .fillMaxWidth()
                    .height(300.dp),
            ) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    CircleIconButton(Icons.AutoMirrored.Filled.ArrowBack, "Back", onBack)
                    CircleIconButton(
                        if (state.isFavorite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                        if (state.isFavorite) "Remove from favorites" else "Save to favorites",
                        onToggleFavorite,
                        tint = if (state.isFavorite) colors.orange else colors.textPrimary,
                    )
                }
                DiscountBadge(deal.discountPercent, Modifier.align(Alignment.BottomEnd).padding(16.dp), large = true)
            }

            Column(Modifier.padding(horizontal = 20.dp, vertical = 20.dp)) {
                Row(verticalAlignment = Alignment.Top) {
                    Column(Modifier.weight(1f)) {
                        Text(deal.title, style = MaterialTheme.typography.headlineSmall, color = colors.textPrimary)
                        Text(
                            "${deal.shopName} · ${deal.area}",
                            style = MaterialTheme.typography.titleSmall,
                            color = colors.green,
                        )
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            formatTaka(deal.price),
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = colors.green,
                        )
                        Text(
                            formatTaka(deal.originalPrice),
                            style = MaterialTheme.typography.bodyMedium,
                            color = colors.textMuted,
                            textDecoration = TextDecoration.LineThrough,
                        )
                    }
                }
                Spacer(Modifier.height(14.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    InfoChip(Icons.Filled.Star, "${deal.rating} (${deal.reviewCount})")
                    InfoChip(Icons.Filled.Place, formatDistance(deal.distanceKm), iconTint = colors.danger)
                    when (status) {
                        DealStatus.LIVE -> InfoChip(
                            null,
                            if (deal.quantityLeft <= 3) "Only ${deal.quantityLeft} left" else "${deal.quantityLeft} left",
                            background = colors.orangeTint, content = colors.orange, bordered = false,
                        )
                        DealStatus.SOLD_OUT -> InfoChip(null, "Sold out", background = colors.dangerTint, content = colors.danger, bordered = false)
                        DealStatus.EXPIRED -> InfoChip(null, "Ended", background = colors.dangerTint, content = colors.danger, bordered = false)
                    }
                }
                Spacer(Modifier.height(18.dp))
                PickupWindowCard(deal, now)
                Spacer(Modifier.height(18.dp))
                Text(deal.description, style = MaterialTheme.typography.bodyLarge, color = colors.textMuted)
                Spacer(Modifier.height(20.dp))
                if (status == DealStatus.LIVE) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Quantity", style = MaterialTheme.typography.titleMedium, color = colors.textPrimary, modifier = Modifier.weight(1f))
                        QuantityStepper(state.quantity, deal.quantityLeft, onDecrement, onIncrement)
                    }
                }
            }
        }

        HorizontalDivider(color = colors.outline)
        Row(
            Modifier
                .fillMaxWidth()
                .background(colors.card)
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.padding(end = 16.dp)) {
                Text("Total", style = MaterialTheme.typography.bodySmall, color = colors.textMuted)
                Text(formatTaka(state.total), style = MaterialTheme.typography.headlineSmall, color = colors.textPrimary)
            }
            PrimaryButton(
                text = when (status) {
                    DealStatus.LIVE -> "Reserve now"
                    DealStatus.SOLD_OUT -> "Sold out"
                    DealStatus.EXPIRED -> "Pickup window closed"
                },
                onClick = onReserve,
                enabled = status == DealStatus.LIVE,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun PickupWindowCard(deal: Deal, now: Long) {
    val colors = FreshSaveTheme.colors
    val remaining = deal.pickupEndMillis - now
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(colors.greenTint)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(44.dp)
                .background(colors.green, RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Outlined.Schedule, null, tint = Color.White)
        }
        Spacer(Modifier.width(14.dp))
        Column {
            Text(
                "Pick up ${formatDayWord(deal.pickupStartMillis, now)} · ${formatTimeRange(deal.pickupStartMillis, deal.pickupEndMillis)}",
                style = MaterialTheme.typography.titleSmall,
                color = colors.textPrimary,
            )
            Text(
                when {
                    remaining <= 0 -> "Reservations closed"
                    deal.pickupStartMillis > now -> "Opens in ${formatTimeLeft(deal.pickupStartMillis - now)}"
                    else -> "Reserve closes in ${formatTimeLeft(remaining)}"
                },
                style = MaterialTheme.typography.labelMedium,
                color = colors.green,
            )
        }
    }
}

@Composable
private fun InfoChip(
    icon: ImageVector?,
    text: String,
    iconTint: Color = FreshSaveTheme.colors.textPrimary,
    background: Color = FreshSaveTheme.colors.card,
    content: Color = FreshSaveTheme.colors.textPrimary,
    bordered: Boolean = true,
) {
    val colors = FreshSaveTheme.colors
    Row(
        Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(background)
            .then(if (bordered) Modifier.border(1.dp, colors.outline, RoundedCornerShape(10.dp)) else Modifier)
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) {
            Icon(icon, null, tint = iconTint, modifier = Modifier.size(14.dp))
            Spacer(Modifier.width(4.dp))
        }
        Text(text, style = MaterialTheme.typography.labelMedium, color = content)
    }
}

@Composable
private fun QuantityStepper(value: Int, max: Int, onDecrement: () -> Unit, onIncrement: () -> Unit) {
    val colors = FreshSaveTheme.colors
    Row(
        Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(colors.card)
            .border(1.dp, colors.outline, RoundedCornerShape(12.dp)),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        StepButton(Icons.Filled.Remove, "Fewer", enabled = value > 1, onClick = onDecrement)
        Text("$value", style = MaterialTheme.typography.titleMedium, color = colors.textPrimary, modifier = Modifier.padding(horizontal = 8.dp))
        StepButton(Icons.Filled.Add, "More", enabled = value < max, onClick = onIncrement)
    }
}

@Composable
private fun StepButton(icon: ImageVector, description: String, enabled: Boolean, onClick: () -> Unit) {
    val colors = FreshSaveTheme.colors
    Box(
        Modifier
            .size(44.dp)
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, description, tint = if (enabled) colors.green else colors.outline, modifier = Modifier.size(18.dp))
    }
}

@Preview(widthDp = 380, heightDp = 820)
@Composable
private fun DealDetailPreview() {
    FreshSaveTheme {
        DealDetailScreen(
            DealDetailUiState(loading = false, deal = MockData.deals(System.currentTimeMillis()).first()),
            {}, {}, {}, {}, {},
        )
    }
}
