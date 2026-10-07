package com.freshsave.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.freshsave.app.data.fake.MockData
import com.freshsave.app.data.model.Deal
import com.freshsave.app.data.model.DealStatus
import com.freshsave.app.ui.theme.FreshSaveTheme
import com.freshsave.app.util.formatDistance

val CardShape = RoundedCornerShape(16.dp)

/** White rounded card with a hairline border — the basic container on every screen. */
@Composable
fun FsCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    shape: Shape = CardShape,
    color: Color = FreshSaveTheme.colors.card,
    borderColor: Color = FreshSaveTheme.colors.outline,
    contentPadding: Int = 16,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = modifier
            .clip(shape)
            .background(color)
            .border(1.dp, borderColor, shape)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(contentPadding.dp),
        content = content,
    )
}

/**
 * The listing card from the Home feed: photo with discount + timer badges on top,
 * shop / item / price underneath. Greys out when sold out or expired.
 */
@Composable
fun DealCard(
    deal: Deal,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = FreshSaveTheme.colors
    val now = LocalNow.current
    val status = deal.status(now)
    val available = status == DealStatus.LIVE
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(CardShape)
            .background(colors.card)
            .border(1.dp, colors.outline, CardShape)
            .clickable(onClick = onClick),
    ) {
        DealImage(
            imageUrl = deal.imageUrl,
            label = deal.imageLabel,
            modifier = Modifier
                .fillMaxWidth()
                .height(132.dp),
        ) {
            DiscountBadge(deal.discountPercent, Modifier.align(Alignment.TopStart).padding(12.dp))
            when (status) {
                DealStatus.LIVE -> TimerBadge(deal.pickupEndMillis, Modifier.align(Alignment.TopEnd).padding(12.dp))
                DealStatus.SOLD_OUT -> StatusPill("Sold out", Modifier.align(Alignment.TopEnd).padding(12.dp),
                    background = Color(0xCC1A1F1C), content = Color.White)
                DealStatus.EXPIRED -> StatusPill("Ended", Modifier.align(Alignment.TopEnd).padding(12.dp),
                    background = Color(0xCC1A1F1C), content = Color.White)
            }
            if (!available) {
                Box(Modifier.matchParentSize().background(colors.card.copy(alpha = 0.35f)))
            }
        }
        Column(Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    deal.shopName,
                    style = MaterialTheme.typography.titleMedium,
                    color = colors.textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                RatingDistance(deal.rating, deal.distanceKm)
            }
            Text(
                "${deal.title} · ${if (available) "${deal.quantityLeft} left" else if (status == DealStatus.SOLD_OUT) "sold out" else "ended"}",
                style = MaterialTheme.typography.bodyMedium,
                color = colors.textMuted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(6.dp))
            PriceTag(deal.price, deal.originalPrice)
        }
    }
}

/** "★4.8 · 0.6 km" */
@Composable
fun RatingDistance(rating: Double, distanceKm: Double, modifier: Modifier = Modifier) {
    Row(modifier, verticalAlignment = Alignment.CenterVertically) {
        Icon(Icons.Filled.Star, null, tint = FreshSaveTheme.colors.textPrimary, modifier = Modifier.size(13.dp))
        Text(
            "$rating · ${formatDistance(distanceKm)}",
            style = MaterialTheme.typography.labelMedium,
            color = FreshSaveTheme.colors.textPrimary,
        )
    }
}

/** A number with a caption under it ("23 meals rescued", "18 items listed"). */
@Composable
fun StatTile(
    value: String,
    label: String,
    modifier: Modifier = Modifier,
    valueColor: Color = FreshSaveTheme.colors.green,
    centered: Boolean = false,
    onClick: (() -> Unit)? = null,
) {
    FsCard(modifier = modifier, onClick = onClick, contentPadding = 14) {
        val align = if (centered) Alignment.CenterHorizontally else Alignment.Start
        Text(
            value,
            style = MaterialTheme.typography.titleLarge,
            color = valueColor,
            modifier = Modifier.align(align),
            maxLines = 1,
        )
        Text(
            label,
            style = MaterialTheme.typography.bodySmall,
            color = FreshSaveTheme.colors.textMuted,
            textAlign = if (centered) TextAlign.Center else TextAlign.Start,
            modifier = Modifier.align(align),
        )
    }
}

/** Dark "CODE 4F29" card with a real QR code; shown on order cards. */
@Composable
fun PickupCodeCard(
    code: String,
    qrContent: String,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(FreshSaveTheme.colors.darkCard)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                "CODE",
                style = MaterialTheme.typography.labelSmall,
                color = Color.White.copy(alpha = 0.6f),
            )
            Text(
                code.toCharArray().joinToString(" "),
                style = MaterialTheme.typography.headlineSmall,
                color = Color.White,
            )
        }
        QrCode(
            content = qrContent,
            modifier = Modifier.size(48.dp),
            foreground = Color.White,
            background = Color.Transparent,
        )
    }
}

@Preview(widthDp = 360)
@Composable
private fun DealCardPreview() {
    FreshSaveTheme {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            DealCard(MockData.deals(System.currentTimeMillis()).first(), onClick = {})
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StatTile("23", "meals rescued", Modifier.width(100.dp))
                StatTile("18 kg", "food saved", Modifier.width(100.dp), valueColor = FreshSaveTheme.colors.orange)
            }
            PickupCodeCard("4F29", "FRESHSAVE:o-4f29:4F29")
        }
    }
}
