package com.freshsave.app.feature.confirmation

import android.content.ActivityNotFoundException
import android.content.Intent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.Place
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.freshsave.app.data.fake.MockData
import com.freshsave.app.data.model.Order
import com.freshsave.app.data.model.OrderStatus
import com.freshsave.app.data.repository.OrderRepository
import com.freshsave.app.navigation.LocalSnackbarHostState
import com.freshsave.app.ui.appViewModel
import com.freshsave.app.ui.components.CircleIconButton
import com.freshsave.app.ui.components.LocalNow
import com.freshsave.app.ui.components.PrimaryButton
import com.freshsave.app.ui.components.QrCode
import com.freshsave.app.ui.components.SecondaryButton
import com.freshsave.app.ui.theme.FreshSaveTheme
import com.freshsave.app.ui.theme.StatusBarIcons
import com.freshsave.app.util.formatClockTime
import com.freshsave.app.util.formatDayWord
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class ConfirmationUiState(val loading: Boolean = true, val order: Order? = null)

class OrderConfirmationViewModel(orderId: String, orderRepository: OrderRepository) : ViewModel() {
    val state: StateFlow<ConfirmationUiState> = orderRepository.observeOrder(orderId)
        .map { ConfirmationUiState(loading = false, order = it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ConfirmationUiState())
}

/** QR content staff scan at pickup. A real backend would sign this. */
fun qrPayloadFor(order: Order) = "FRESHSAVE:${order.id}:${order.pickupCode}"

@Composable
fun OrderConfirmationRoute(orderId: String, onClose: () -> Unit, onViewOrder: () -> Unit) {
    val viewModel = appViewModel(key = "order-$orderId") { OrderConfirmationViewModel(orderId, it.orderRepository) }
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val snackbar = LocalSnackbarHostState.current
    val scope = rememberCoroutineScope()
    OrderConfirmationScreen(
        state = state,
        onClose = onClose,
        onViewOrder = onViewOrder,
        onDirections = { order ->
            // Opens Google Maps (or any maps app) searching for the shop.
            val uri = "geo:0,0?q=${order.shopName}, ${order.area}, Dhaka".toUri()
            try {
                context.startActivity(Intent(Intent.ACTION_VIEW, uri))
            } catch (_: ActivityNotFoundException) {
                scope.launch { snackbar.showSnackbar("No maps app installed.") }
            }
        },
    )
}

@Composable
fun OrderConfirmationScreen(
    state: ConfirmationUiState,
    onClose: () -> Unit,
    onViewOrder: () -> Unit,
    onDirections: (Order) -> Unit,
) {
    StatusBarIcons(lightIcons = true)
    val colors = FreshSaveTheme.colors
    val order = state.order
    if (order == null) {
        Box(Modifier.fillMaxSize().background(colors.background), contentAlignment = Alignment.Center) {
            if (state.loading) CircularProgressIndicator(color = colors.green) else Text("Order not found", color = colors.textMuted)
        }
        return
    }
    val now = LocalNow.current
    val status = order.effectiveStatus(now)
    val (title, subtitle) = when (status) {
        OrderStatus.ACTIVE -> "Reserved!" to "Show this code at pickup"
        OrderStatus.COLLECTED -> "Collected" to "Enjoy your rescue 🌱"
        OrderStatus.MISSED -> "Pickup missed" to "The pickup window has closed"
    }

    Box(Modifier.fillMaxSize().background(colors.background)) {
        // Green header band behind the top of the ticket card.
        Box(
            Modifier
                .fillMaxWidth()
                .height(330.dp)
                .background(Brush.verticalGradient(listOf(Color(0xFF2E9A62), colors.green, colors.greenDark))),
        )
        Column(
            Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding(),
        ) {
            CircleIconButton(
                Icons.AutoMirrored.Filled.ArrowBack, "Back", onClose,
                modifier = Modifier.padding(start = 16.dp, top = 8.dp),
                tint = Color.White,
                background = Color.White.copy(alpha = 0.15f),
            )
            Column(
                Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Box(
                    Modifier
                        .size(76.dp)
                        .background(Color.White.copy(alpha = 0.18f), CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        if (status == OrderStatus.MISSED) Icons.Filled.Close else Icons.Filled.Check,
                        null, tint = Color.White, modifier = Modifier.size(38.dp),
                    )
                }
                Spacer(Modifier.height(14.dp))
                Text(title, style = MaterialTheme.typography.headlineMedium, color = Color.White)
                Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = Color.White.copy(alpha = 0.85f))
                Spacer(Modifier.height(22.dp))

                Column(
                    Modifier
                        .fillMaxWidth()
                        .shadow(10.dp, RoundedCornerShape(24.dp))
                        .background(colors.card, RoundedCornerShape(24.dp))
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    QrCode(
                        content = qrPayloadFor(order),
                        modifier = Modifier.size(170.dp),
                        foreground = if (status == OrderStatus.ACTIVE) Color(0xFF1A1F1C) else Color(0xFFBDBDBD),
                    )
                    Spacer(Modifier.height(18.dp))
                    Text("PICKUP CODE", style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 3.sp), color = colors.textMuted)
                    Text(
                        order.pickupCode.toCharArray().joinToString(" "),
                        style = MaterialTheme.typography.displaySmall,
                        color = colors.textPrimary,
                        textAlign = TextAlign.Center,
                    )
                    Spacer(Modifier.height(16.dp))
                    DashedDivider()
                    Spacer(Modifier.height(14.dp))
                    DetailRow("Order", "${if (order.quantity > 1) "${order.quantity}× " else ""}${order.itemTitle}")
                    DetailRow("Pickup by", "${formatClockTime(order.pickupEndMillis)} ${formatDayWord(order.pickupEndMillis, now)}")
                    DetailRow("Location", "${order.shopName}, ${order.area}")
                    DetailRow("Paid", "৳${order.total} · ${order.paymentMethod.label}")
                }
                Spacer(Modifier.height(20.dp))
            }
            Row(
                Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                SecondaryButton("Directions", { onDirections(order) }, Modifier.weight(1f), leadingIcon = Icons.Outlined.Place)
                PrimaryButton("View order", onViewOrder, Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = FreshSaveTheme.colors.textMuted, modifier = Modifier.weight(1f))
        Text(value, style = MaterialTheme.typography.titleSmall, color = FreshSaveTheme.colors.textPrimary, textAlign = TextAlign.End)
    }
}

@Composable
private fun DashedDivider() {
    val color = FreshSaveTheme.colors.outline
    Canvas(Modifier.fillMaxWidth().height(1.dp)) {
        drawLine(color, Offset.Zero, Offset(size.width, 0f), 2f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 8f)))
    }
}

@Preview(widthDp = 380, heightDp = 820)
@Composable
private fun ConfirmationPreview() {
    FreshSaveTheme {
        OrderConfirmationScreen(
            ConfirmationUiState(false, MockData.orders(System.currentTimeMillis()).first()),
            {}, {}, {},
        )
    }
}
