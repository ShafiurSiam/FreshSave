package com.freshsave.app.feature.business.queue

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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.TaskAlt
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.freshsave.app.data.AppClock
import com.freshsave.app.data.fake.MockData
import com.freshsave.app.data.model.Order
import com.freshsave.app.data.model.OrderStatus
import com.freshsave.app.data.repository.BusinessRepository
import com.freshsave.app.data.repository.OrderRepository
import com.freshsave.app.navigation.LocalSnackbarHostState
import com.freshsave.app.ui.appViewModel
import com.freshsave.app.ui.components.Avatar
import com.freshsave.app.ui.components.EmptyState
import com.freshsave.app.ui.components.FsCard
import com.freshsave.app.ui.components.FsTextField
import com.freshsave.app.ui.components.PrimaryButton
import com.freshsave.app.ui.components.ScreenTitle
import com.freshsave.app.ui.components.SegmentedTabs
import com.freshsave.app.ui.components.StatusPill
import com.freshsave.app.ui.components.avatarColorFor
import com.freshsave.app.ui.theme.FreshSaveTheme
import com.freshsave.app.util.formatClockTime
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class QueueUiState(
    val loading: Boolean = true,
    val toFulfil: List<Order> = emptyList(),
    val collected: List<Order> = emptyList(),
)

class PickupQueueViewModel(
    clock: AppClock,
    businessRepository: BusinessRepository,
    private val orderRepository: OrderRepository,
) : ViewModel() {
    private val tick = clock.now.map { it / 10_000 }.distinctUntilChanged()

    val state: StateFlow<QueueUiState> = combine(
        orderRepository.observeShopOrders(businessRepository.currentShopId), tick,
    ) { orders, _ ->
        val now = System.currentTimeMillis()
        QueueUiState(
            loading = false,
            toFulfil = orders.filter { it.effectiveStatus(now) == OrderStatus.ACTIVE }.sortedBy { it.pickupEndMillis },
            collected = orders.filter { it.status == OrderStatus.COLLECTED }.sortedByDescending { it.collectedAtMillis },
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), QueueUiState())

    /** Staff type the code the customer shows; only a match closes the order. */
    fun verifyAndCollect(order: Order, enteredCode: String): Boolean {
        if (!enteredCode.trim().equals(order.pickupCode, ignoreCase = true)) return false
        viewModelScope.launch { orderRepository.markCollected(order.id) }
        return true
    }
}

@Composable
fun PickupQueueRoute() {
    val viewModel = appViewModel { PickupQueueViewModel(it.clock, it.businessRepository, it.orderRepository) }
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbar = LocalSnackbarHostState.current
    val scope = rememberCoroutineScope()
    var verifying by remember { mutableStateOf<Order?>(null) }

    PickupQueueScreen(state, onMarkCollected = { verifying = it })

    verifying?.let { order ->
        VerifyCodeDialog(
            order = order,
            onDismiss = { verifying = null },
            onSubmit = { code ->
                val ok = viewModel.verifyAndCollect(order, code)
                if (ok) {
                    verifying = null
                    scope.launch { snackbar.showSnackbar("${order.customerShortName}'s order collected") }
                }
                ok
            },
        )
    }
}

@Composable
fun PickupQueueScreen(state: QueueUiState, onMarkCollected: (Order) -> Unit) {
    val colors = FreshSaveTheme.colors
    var tab by rememberSaveable { mutableIntStateOf(0) }
    Column(
        Modifier
            .fillMaxSize()
            .background(colors.background)
            .statusBarsPadding(),
    ) {
        Column(Modifier.padding(horizontal = 20.dp, vertical = 12.dp)) {
            ScreenTitle("Pickup queue")
            Spacer(Modifier.height(16.dp))
            SegmentedTabs(listOf("To fulfil (${state.toFulfil.size})", "Collected"), tab, { tab = it })
        }
        val list = if (tab == 0) state.toFulfil else state.collected
        LazyColumn(
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 4.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (!state.loading && list.isEmpty()) {
                item {
                    if (tab == 0) EmptyState("Queue is clear", "New reservations will appear here.", icon = Icons.Outlined.TaskAlt)
                    else EmptyState("Nothing collected yet", "Orders you hand over show up here.", icon = Icons.Outlined.Inventory2)
                }
            }
            items(list, key = { it.id }) { order ->
                QueueCard(order, collected = tab == 1, onMarkCollected = { onMarkCollected(order) }, modifier = Modifier.animateItem())
            }
        }
    }
}

@Composable
private fun QueueCard(order: Order, collected: Boolean, onMarkCollected: () -> Unit, modifier: Modifier = Modifier) {
    val colors = FreshSaveTheme.colors
    FsCard(modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Avatar(order.customerName.take(1), color = avatarColorFor(order.customerName))
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(order.customerShortName, style = MaterialTheme.typography.titleMedium, color = colors.textPrimary)
                Text("${order.itemTitle} ×${order.quantity}", style = MaterialTheme.typography.bodySmall, color = colors.textMuted)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text("Code", style = MaterialTheme.typography.labelSmall, color = colors.textMuted)
                Text(order.pickupCode.toCharArray().joinToString(" "), style = MaterialTheme.typography.titleLarge, color = colors.textPrimary)
            }
        }
        Spacer(Modifier.height(12.dp))
        if (collected) {
            StatusPill("Collected at ${formatClockTime(order.collectedAtMillis ?: 0)}")
        } else {
            PrimaryButton(
                "Mark collected",
                onMarkCollected,
                Modifier.fillMaxWidth().height(48.dp),
                leadingIcon = Icons.Filled.Check,
            )
        }
    }
}

@Composable
private fun VerifyCodeDialog(order: Order, onDismiss: () -> Unit, onSubmit: (String) -> Boolean) {
    var code by remember { mutableStateOf("") }
    var error by remember { mutableStateOf(false) }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = FreshSaveTheme.colors.card,
        title = { Text("Verify pickup code") },
        text = {
            Column {
                Text("Ask ${order.customerShortName} to show their code and type it here.")
                Spacer(Modifier.height(12.dp))
                FsTextField(
                    value = code,
                    onValueChange = {
                        code = it.uppercase().filter(Char::isLetterOrDigit).take(4)
                        error = false
                    },
                    placeholder = "e.g. 4F29",
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters),
                    isError = error,
                )
                if (error) {
                    Text("That code doesn't match this order.", color = FreshSaveTheme.colors.danger, style = MaterialTheme.typography.bodySmall)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { error = !onSubmit(code) }, enabled = code.length == 4) { Text("Confirm") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

@Preview(widthDp = 380, heightDp = 800)
@Composable
private fun QueuePreview() {
    val orders = MockData.orders(System.currentTimeMillis()).filter { it.shopId == MockData.BUSINESS_SHOP_ID }
    FreshSaveTheme { PickupQueueScreen(QueueUiState(false, orders.filter { it.status == OrderStatus.ACTIVE }, emptyList()), {}) }
}
