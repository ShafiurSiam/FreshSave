package com.freshsave.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.freshsave.app.ui.theme.FreshSaveTheme
import com.freshsave.app.util.formatTaka
import com.freshsave.app.util.formatTimeLeft

/** Orange "-70%" pill. */
@Composable
fun DiscountBadge(percent: Int, modifier: Modifier = Modifier, large: Boolean = false) {
    Text(
        text = "-$percent%",
        color = Color.White,
        style = if (large) MaterialTheme.typography.titleMedium else MaterialTheme.typography.labelLarge,
        modifier = modifier
            .background(FreshSaveTheme.colors.orange, RoundedCornerShape(10.dp))
            .padding(horizontal = if (large) 12.dp else 9.dp, vertical = 4.dp),
    )
}

enum class TimerStyle { Dark, Tint }

/**
 * Live "⏱ 42 min left" badge. It reads [LocalNow], so it re-renders every second by itself.
 * Dark sits on top of photos; Tint is the orange-on-cream version used in order cards.
 */
@Composable
fun TimerBadge(endMillis: Long, modifier: Modifier = Modifier, style: TimerStyle = TimerStyle.Dark) {
    val now = LocalNow.current
    val remaining = endMillis - now
    val text = if (remaining <= 0) "Ended" else formatTimeLeft(remaining) + " left"
    val (bg, fg) = when (style) {
        TimerStyle.Dark -> Color(0xCC1A1F1C) to Color.White
        TimerStyle.Tint -> FreshSaveTheme.colors.orangeTint to FreshSaveTheme.colors.orange
    }
    Row(
        modifier = modifier
            .background(bg, RoundedCornerShape(8.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        Icon(Icons.Outlined.Timer, contentDescription = null, tint = fg, modifier = Modifier.size(13.dp))
        Text(text, color = fg, style = MaterialTheme.typography.labelMedium)
    }
}

/** Small rounded label such as "Live", "Reserved", "Sold out", "Verified partner". */
@Composable
fun StatusPill(
    text: String,
    modifier: Modifier = Modifier,
    background: Color = FreshSaveTheme.colors.greenTint,
    content: Color = FreshSaveTheme.colors.green,
    showDot: Boolean = false,
) {
    Row(
        modifier = modifier
            .background(background, RoundedCornerShape(50))
            .padding(horizontal = 10.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (showDot) {
            androidx.compose.foundation.layout.Box(
                Modifier
                    .padding(end = 5.dp)
                    .size(6.dp)
                    .background(content, CircleShape),
            )
        }
        Text(text, color = content, style = MaterialTheme.typography.labelMedium)
    }
}

/** "৳75 ~~৳250~~": sale price in green with the original struck through. */
@Composable
fun PriceTag(price: Int, originalPrice: Int?, modifier: Modifier = Modifier, large: Boolean = false) {
    Row(modifier, verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            formatTaka(price),
            color = FreshSaveTheme.colors.green,
            style = if (large) MaterialTheme.typography.headlineSmall else MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.ExtraBold,
        )
        if (originalPrice != null && originalPrice > price) {
            Text(
                formatTaka(originalPrice),
                color = FreshSaveTheme.colors.textMuted,
                style = MaterialTheme.typography.bodyMedium,
                textDecoration = TextDecoration.LineThrough,
                modifier = Modifier.padding(bottom = 2.dp),
            )
        }
    }
}

@Preview
@Composable
private fun BadgesPreview() {
    FreshSaveTheme {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(12.dp)) {
            DiscountBadge(70)
            TimerBadge(System.currentTimeMillis() + 42 * 60_000)
            TimerBadge(System.currentTimeMillis() + 38 * 60_000, style = TimerStyle.Tint)
            PriceTag(75, 250)
        }
    }
}
