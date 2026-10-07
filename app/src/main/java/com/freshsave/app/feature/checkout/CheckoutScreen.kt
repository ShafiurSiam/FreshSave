package com.freshsave.app.feature.checkout

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.CreditCard
import androidx.compose.material.icons.outlined.Place
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.freshsave.app.data.fake.MockData
import com.freshsave.app.data.model.PaymentMethod
import com.freshsave.app.ui.appViewModel
import com.freshsave.app.ui.components.FsCard
import com.freshsave.app.ui.components.FsTopBar
import com.freshsave.app.ui.components.LocalNow
import com.freshsave.app.ui.components.PrimaryButton
import com.freshsave.app.ui.components.Thumbnail
import com.freshsave.app.ui.theme.FreshSaveTheme
import com.freshsave.app.util.formatDayWord
import com.freshsave.app.util.formatTaka
import com.freshsave.app.util.formatTimeLeft
import com.freshsave.app.util.formatTimeRange

@Composable
fun CheckoutRoute(
    dealId: String,
    quantity: Int,
    onBack: () -> Unit,
    onOrderPlaced: (orderId: String) -> Unit,
) {
    val viewModel = appViewModel(key = "checkout-$dealId-$quantity") {
        CheckoutViewModel(dealId, quantity, it.clock, it.dealRepository, it.orderRepository)
    }
    val state by viewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(state.placedOrderId) {
        state.placedOrderId?.let {
            viewModel.onNavigated()
            onOrderPlaced(it)
        }
    }
    CheckoutScreen(state, onBack, viewModel::selectMethod, viewModel::pay)
}

@Composable
fun CheckoutScreen(
    state: CheckoutUiState,
    onBack: () -> Unit,
    onSelectMethod: (PaymentMethod) -> Unit,
    onPay: () -> Unit,
) {
    val colors = FreshSaveTheme.colors
    val now = LocalNow.current
    val deal = state.deal
    val holdLeft = state.holdEndsAtMillis - now
    val holdExpired = holdLeft <= 0

    Column(Modifier.fillMaxSize().background(colors.background)) {
        FsTopBar("Checkout", Modifier.statusBarsPadding(), onNavClick = onBack)
        Column(
            Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
        ) {
            if (deal != null) {
                FsCard(contentPadding = 14) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Thumbnail(deal.imageUrl, size = 56)
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(deal.title, style = MaterialTheme.typography.titleMedium, color = colors.textPrimary)
                            Text("${deal.shopName} · Qty ${state.quantity}", style = MaterialTheme.typography.bodySmall, color = colors.textMuted)
                        }
                        Text(formatTaka(state.subtotal), style = MaterialTheme.typography.titleMedium, color = colors.green)
                    }
                }
                Spacer(Modifier.height(12.dp))
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(colors.greenTint)
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.Outlined.Place, null, tint = colors.green, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(10.dp))
                    Text(
                        "Pick up at ${deal.shopName}, ${deal.area} · ${formatTimeRange(deal.pickupStartMillis, deal.pickupEndMillis)} " +
                            formatDayWord(deal.pickupStartMillis, now),
                        style = MaterialTheme.typography.labelMedium,
                        color = colors.textPrimary,
                    )
                }
            }

            Spacer(Modifier.height(22.dp))
            Text("Payment method", style = MaterialTheme.typography.titleMedium, color = colors.textPrimary)
            Spacer(Modifier.height(10.dp))
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                PaymentOption(PaymentMethod.BKASH, "bKash · 017**-***678", state.paymentMethod == PaymentMethod.BKASH) { onSelectMethod(PaymentMethod.BKASH) }
                PaymentOption(PaymentMethod.NAGAD, "Nagad", state.paymentMethod == PaymentMethod.NAGAD) { onSelectMethod(PaymentMethod.NAGAD) }
                PaymentOption(PaymentMethod.CARD, "Card · Visa/Mastercard", state.paymentMethod == PaymentMethod.CARD) { onSelectMethod(PaymentMethod.CARD) }
            }

            Spacer(Modifier.height(20.dp))
            SummaryRow("Subtotal", formatTaka(state.subtotal))
            SummaryRow("Service fee", formatTaka(state.serviceFee))
            SummaryRow("You save", formatTaka(state.savings), color = colors.green, bold = true)

            Spacer(Modifier.height(12.dp))
            Text(
                if (holdExpired) "Your hold expired — go back and reserve again."
                else "We're holding this for you · ${formatTimeLeft(holdLeft)}",
                style = MaterialTheme.typography.labelMedium,
                color = if (holdExpired) colors.danger else colors.orange,
            )
            if (state.error != null) {
                Spacer(Modifier.height(8.dp))
                Text(state.error, style = MaterialTheme.typography.bodyMedium, color = colors.danger)
            }
            Spacer(Modifier.height(20.dp))
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
                text = "Pay ${formatTaka(state.total)} with ${state.paymentMethod.label}",
                onClick = onPay,
                loading = state.paying,
                enabled = deal != null && !holdExpired,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun PaymentOption(method: PaymentMethod, label: String, selected: Boolean, onClick: () -> Unit) {
    val colors = FreshSaveTheme.colors
    val shape = RoundedCornerShape(14.dp)
    Row(
        Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(colors.card)
            .border(if (selected) 2.dp else 1.dp, if (selected) colors.green else colors.outline, shape)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        PaymentLogo(method)
        Spacer(Modifier.width(12.dp))
        Text(label, style = MaterialTheme.typography.titleSmall, color = colors.textPrimary, modifier = Modifier.weight(1f))
        if (selected) {
            Box(Modifier.size(22.dp).background(colors.green, CircleShape), contentAlignment = Alignment.Center) {
                Icon(Icons.Filled.Check, null, tint = Color.White, modifier = Modifier.size(14.dp))
            }
        } else {
            Box(Modifier.size(22.dp).border(2.dp, colors.outline, CircleShape))
        }
    }
}

/** Coloured squares standing in for the bKash / Nagad / card brand marks. */
@Composable
private fun PaymentLogo(method: PaymentMethod) {
    val (bg, text) = when (method) {
        PaymentMethod.BKASH -> Color(0xFFE2136E) to "bKash"
        PaymentMethod.NAGAD -> Color(0xFFF6921E) to "Nagad"
        PaymentMethod.CARD -> Color(0xFF6366F1) to null
    }
    Box(Modifier.size(40.dp).background(bg, RoundedCornerShape(10.dp)), contentAlignment = Alignment.Center) {
        if (text != null) {
            Text(text, color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
        } else {
            Icon(Icons.Outlined.CreditCard, null, tint = Color.White, modifier = Modifier.size(20.dp))
        }
    }
}

@Composable
private fun SummaryRow(label: String, value: String, color: Color = FreshSaveTheme.colors.textMuted, bold: Boolean = false) {
    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Text(
            label,
            style = if (bold) MaterialTheme.typography.titleSmall else MaterialTheme.typography.bodyMedium,
            color = color,
            modifier = Modifier.weight(1f),
        )
        Text(value, style = if (bold) MaterialTheme.typography.titleSmall else MaterialTheme.typography.bodyMedium, color = color)
    }
}

@Preview(widthDp = 380, heightDp = 820)
@Composable
private fun CheckoutPreview() {
    FreshSaveTheme {
        CheckoutScreen(
            CheckoutUiState(deal = MockData.deals(System.currentTimeMillis()).first(), holdEndsAtMillis = System.currentTimeMillis() + 600_000),
            {}, {}, {},
        )
    }
}
