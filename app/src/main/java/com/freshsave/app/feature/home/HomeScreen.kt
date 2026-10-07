package com.freshsave.app.feature.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Place
import androidx.compose.material.icons.outlined.SearchOff
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.freshsave.app.data.fake.MockData
import com.freshsave.app.ui.appViewModel
import com.freshsave.app.ui.components.CardShape
import com.freshsave.app.ui.components.DealCard
import com.freshsave.app.ui.components.EmptyState
import com.freshsave.app.ui.components.SectionHeader
import com.freshsave.app.ui.components.SkeletonDealCard
import com.freshsave.app.ui.theme.FreshSaveTheme

@Composable
fun HomeRoute(
    onDealClick: (String) -> Unit,
    onNotificationsClick: () -> Unit,
    onOpenMap: () -> Unit,
    onSeeAll: () -> Unit,
) {
    val viewModel = appViewModel {
        HomeViewModel(it.clock, it.dealRepository, it.shopRepository, it.notificationRepository, it.userRepository)
    }
    val state by viewModel.state.collectAsStateWithLifecycle()
    HomeScreen(
        state = state,
        onDealClick = onDealClick,
        onNotificationsClick = onNotificationsClick,
        onOpenMap = onOpenMap,
        onSeeAll = onSeeAll,
        onAreaSelected = viewModel::selectArea,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    state: HomeUiState,
    onDealClick: (String) -> Unit,
    onNotificationsClick: () -> Unit,
    onOpenMap: () -> Unit,
    onSeeAll: () -> Unit,
    onAreaSelected: (String) -> Unit,
) {
    val colors = FreshSaveTheme.colors
    var showAreaPicker by remember { mutableStateOf(false) }

    // LazyColumn only composes the cards that are on screen, so long lists stay smooth.
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
            .statusBarsPadding(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            HomeHeader(
                greeting = "${state.greeting}, ${state.firstName} 👋",
                area = state.area,
                unread = state.unreadNotifications,
                onAreaClick = { showAreaPicker = true },
                onBellClick = onNotificationsClick,
            )
        }
        item {
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(150.dp)
                    .clip(CardShape),
            ) {
                MapPlaceholder(
                    pins = state.pins,
                    onPinClick = { onDealClick(it.dealId) },
                    modifier = Modifier.fillMaxSize(),
                )
                Row(
                    Modifier
                        .align(Alignment.BottomEnd)
                        .padding(10.dp)
                        .clip(RoundedCornerShape(50))
                        .background(colors.card)
                        .clickable(onClick = onOpenMap)
                        .padding(start = 12.dp, end = 6.dp, top = 6.dp, bottom = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("Open full map", style = MaterialTheme.typography.labelMedium, color = colors.green)
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = colors.green, modifier = Modifier.size(16.dp))
                }
            }
        }
        item {
            SectionHeader("Ending soon", action = "See all", onAction = onSeeAll, modifier = Modifier.padding(top = 6.dp))
        }
        if (state.loading) {
            items(3) { SkeletonDealCard() }
        }
        if (!state.loading && state.endingSoon.isEmpty()) {
            item {
                EmptyState(
                    title = "All rescued for now",
                    message = "No deals near ${state.area} right now. Check back closer to closing time.",
                    icon = Icons.Outlined.SearchOff,
                )
            }
        }
        items(state.endingSoon, key = { it.id }) { deal ->
            DealCard(deal = deal, onClick = { onDealClick(deal.id) }, modifier = Modifier.animateItem())
        }
    }

    if (showAreaPicker) {
        ModalBottomSheet(onDismissRequest = { showAreaPicker = false }, containerColor = colors.card) {
            Column(Modifier.padding(bottom = 32.dp)) {
                Text(
                    "Choose your area",
                    style = MaterialTheme.typography.titleLarge,
                    color = colors.textPrimary,
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
                )
                dhakaAreas.forEach { area ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clickable {
                                onAreaSelected(area)
                                showAreaPicker = false
                            }
                            .padding(horizontal = 24.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(Icons.Outlined.Place, null, tint = colors.textMuted)
                        Spacer(Modifier.size(12.dp))
                        Text(area, style = MaterialTheme.typography.bodyLarge, color = colors.textPrimary, modifier = Modifier.weight(1f))
                        if (area == state.area) Icon(Icons.Filled.Check, null, tint = colors.green)
                    }
                }
            }
        }
    }
}

@Composable
private fun HomeHeader(
    greeting: String,
    area: String,
    unread: Int,
    onAreaClick: () -> Unit,
    onBellClick: () -> Unit,
) {
    val colors = FreshSaveTheme.colors
    Row(verticalAlignment = Alignment.CenterVertically) {
        Column(
            Modifier
                .weight(1f)
                .clip(RoundedCornerShape(8.dp))
                .clickable(onClick = onAreaClick),
        ) {
            Text(greeting, style = MaterialTheme.typography.bodyMedium, color = colors.textMuted)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Rescue near $area", style = MaterialTheme.typography.titleLarge, color = colors.textPrimary)
                Icon(Icons.Filled.KeyboardArrowDown, "Change area", tint = colors.textPrimary)
            }
        }
        Box(
            Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(colors.greenTint)
                .clickable(onClick = onBellClick),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Outlined.Notifications, "Notifications", tint = colors.green)
            if (unread > 0) {
                Box(
                    Modifier
                        .align(Alignment.TopEnd)
                        .padding(10.dp)
                        .size(9.dp)
                        .background(colors.orange, CircleShape),
                )
            }
        }
    }
}

@Preview(widthDp = 380, heightDp = 800)
@Composable
private fun HomePreview() {
    val deals = MockData.deals(System.currentTimeMillis()).take(3)
    FreshSaveTheme {
        HomeScreen(
            state = HomeUiState(
                loading = false, greeting = "Good evening", firstName = "Ayesha", unreadNotifications = 2,
                endingSoon = deals, pins = listOf(MapPin("tasty-bag", 75, 0.3f, 0.4f, true), MapPin("x", 150, 0.8f, 0.3f, false)),
            ),
            onDealClick = {}, onNotificationsClick = {}, onOpenMap = {}, onSeeAll = {}, onAreaSelected = {},
        )
    }
}
