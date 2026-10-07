package com.freshsave.app.feature.notifications

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.NotificationsNone
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.ShoppingBag
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.freshsave.app.R
import com.freshsave.app.data.fake.MockData
import com.freshsave.app.data.model.AppNotification
import com.freshsave.app.data.model.NotificationType
import com.freshsave.app.data.model.UserRole
import com.freshsave.app.data.repository.NotificationRepository
import com.freshsave.app.data.repository.SessionRepository
import com.freshsave.app.ui.appViewModel
import com.freshsave.app.ui.components.EmptyState
import com.freshsave.app.ui.components.FsTopBar
import com.freshsave.app.ui.components.LocalNow
import com.freshsave.app.ui.theme.FreshSaveTheme
import com.freshsave.app.util.formatAgo
import com.freshsave.app.util.isToday
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class NotificationsUiState(
    val loading: Boolean = true,
    val role: UserRole = UserRole.SHOPPER,
    val items: List<AppNotification> = emptyList(),
)

/** Shared by shoppers and businesses: it shows the inbox for whichever role is active. */
@OptIn(ExperimentalCoroutinesApi::class)
class NotificationsViewModel(
    sessionRepository: SessionRepository,
    private val notificationRepository: NotificationRepository,
) : ViewModel() {
    val state: StateFlow<NotificationsUiState> = sessionRepository.session
        .map { it.role ?: UserRole.SHOPPER }
        .distinctUntilChanged()
        // flatMapLatest swaps to the new role's notification stream if the role changes.
        .flatMapLatest { role ->
            notificationRepository.observe(role).map { NotificationsUiState(false, role, it) }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), NotificationsUiState())

    fun markAllRead() {
        viewModelScope.launch { notificationRepository.markAllRead(state.value.role) }
    }

    fun markRead(id: String) {
        viewModelScope.launch { notificationRepository.markRead(id) }
    }
}

@Composable
fun NotificationsRoute(onBack: () -> Unit, onOpen: (AppNotification) -> Unit) {
    val viewModel = appViewModel { NotificationsViewModel(it.sessionRepository, it.notificationRepository) }
    val state by viewModel.state.collectAsStateWithLifecycle()
    NotificationsScreen(
        state = state,
        onBack = onBack,
        onMarkAllRead = viewModel::markAllRead,
        onOpen = {
            viewModel.markRead(it.id)
            onOpen(it)
        },
    )
}

@Composable
fun NotificationsScreen(
    state: NotificationsUiState,
    onBack: () -> Unit,
    onMarkAllRead: () -> Unit,
    onOpen: (AppNotification) -> Unit,
) {
    val colors = FreshSaveTheme.colors
    val now = LocalNow.current
    val (today, earlier) = state.items.partition { isToday(it.createdAtMillis, now) }
    Column(
        Modifier
            .fillMaxSize()
            .background(colors.background)
            .statusBarsPadding(),
    ) {
        FsTopBar("Notifications", onNavClick = onBack) {
            if (state.items.any { !it.read }) {
                Text(
                    "Mark all read",
                    style = MaterialTheme.typography.titleSmall,
                    color = colors.green,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable(onClick = onMarkAllRead)
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                )
            }
        }
        LazyColumn(
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.navigationBarsPadding(),
        ) {
            if (!state.loading && state.items.isEmpty()) {
                item { EmptyState("You're all caught up", "New deals and order updates will appear here.", icon = Icons.Outlined.NotificationsNone) }
            }
            if (today.isNotEmpty()) {
                item { GroupLabel("TODAY") }
                items(today, key = { it.id }) { NotificationCard(it, now, onOpen, Modifier.animateItem()) }
            }
            if (earlier.isNotEmpty()) {
                item { GroupLabel("EARLIER") }
                items(earlier, key = { it.id }) { NotificationCard(it, now, onOpen, Modifier.animateItem()) }
            }
        }
    }
}

@Composable
private fun GroupLabel(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.labelMedium,
        color = FreshSaveTheme.colors.textMuted,
        modifier = Modifier.padding(top = 10.dp, bottom = 2.dp),
    )
}

@Composable
private fun NotificationCard(item: AppNotification, now: Long, onOpen: (AppNotification) -> Unit, modifier: Modifier = Modifier) {
    val colors = FreshSaveTheme.colors
    val (accent, tint, painter) = styleFor(item.type)
    val shape = RoundedCornerShape(16.dp)
    Row(
        modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
            .clip(shape)
            .background(colors.card)
            .border(1.dp, colors.outline, shape)
            .clickable { onOpen(item) },
    ) {
        // Coloured strip on the left marks unread items, like the mock-up.
        Box(
            Modifier
                .width(4.dp)
                .fillMaxHeight()
                .background(if (item.read) Color.Transparent else accent),
        )
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.Top) {
            Box(
                Modifier
                    .size(40.dp)
                    .background(tint, RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(painter, null, tint = accent, modifier = Modifier.size(20.dp))
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(item.title, style = MaterialTheme.typography.titleSmall, color = colors.textPrimary)
                Text(item.body, style = MaterialTheme.typography.bodySmall, color = colors.textMuted)
            }
            Text(
                formatAgo(item.createdAtMillis, now),
                style = MaterialTheme.typography.labelSmall,
                color = colors.textMuted,
                textAlign = TextAlign.End,
                modifier = Modifier.padding(start = 8.dp),
            )
        }
    }
}

/** Accent colour, icon background and icon for each kind of notification. */
@Composable
private fun styleFor(type: NotificationType): Triple<Color, Color, Painter> {
    val c = FreshSaveTheme.colors
    return when (type) {
        NotificationType.DEAL_ENDING -> Triple(c.orange, c.orangeTint, rememberVectorPainter(Icons.Outlined.Schedule))
        NotificationType.ORDER_UPDATE -> Triple(c.green, c.greenTint, rememberVectorPainter(Icons.Filled.Check))
        NotificationType.NEW_FROM_FAVORITE -> Triple(c.green, c.greenTint, painterResource(R.drawable.ic_leaf))
        NotificationType.REVIEW_PROMPT -> Triple(c.textMuted, c.surfaceMuted, rememberVectorPainter(Icons.Outlined.StarOutline))
        NotificationType.WEEKLY_RECAP -> Triple(c.textMuted, c.surfaceMuted, rememberVectorPainter(Icons.Outlined.BarChart))
        NotificationType.NEW_ORDER -> Triple(c.green, c.greenTint, rememberVectorPainter(Icons.Outlined.ShoppingBag))
        NotificationType.NEW_REVIEW -> Triple(c.orange, c.orangeTint, rememberVectorPainter(Icons.Outlined.StarOutline))
    }
}

@Preview(widthDp = 380, heightDp = 800)
@Composable
private fun NotificationsPreview() {
    FreshSaveTheme {
        NotificationsScreen(
            NotificationsUiState(false, UserRole.SHOPPER, MockData.notifications(System.currentTimeMillis()).filter { it.audience == UserRole.SHOPPER }),
            {}, {}, {},
        )
    }
}
