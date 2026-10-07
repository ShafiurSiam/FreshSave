package com.freshsave.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.freshsave.app.ui.theme.FreshSaveTheme

/**
 * Diagonal-stripe box used wherever a photo will go, like the grey boxes in App.pdf.
 * [label] is the little caption ("bakery bag photo"); pass null to hide it.
 */
@Composable
fun PlaceholderImage(
    label: String?,
    modifier: Modifier = Modifier,
    shape: Shape = RectangleShape,
    labelAlignment: Alignment = Alignment.BottomStart,
    tint: Color? = null,
) {
    val colors = FreshSaveTheme.colors
    val base = tint ?: if (colors.isDark) Color(0xFF2A332D) else Color(0xFFEDE6D8)
    val stripe = if (colors.isDark) Color(0xFF323C35) else Color(0xFFF6F1E8)
    Box(modifier.clip(shape).background(base)) {
        Canvas(Modifier.fillMaxSize()) {
            // Draw 45° stripes across the whole box; spacing scales with density.
            val gap = 14.dp.toPx()
            val strokeWidth = 7.dp.toPx()
            var x = -size.height
            while (x < size.width) {
                drawLine(stripe, Offset(x, size.height), Offset(x + size.height, 0f), strokeWidth)
                x += gap
            }
        }
        if (label != null) {
            Text(
                text = label,
                style = TextStyle(fontFamily = FontFamily.Serif, fontSize = 10.sp, fontWeight = FontWeight.SemiBold),
                color = colors.textMuted,
                modifier = Modifier
                    .align(labelAlignment)
                    .padding(10.dp)
                    .background(colors.card.copy(alpha = 0.85f), RoundedCornerShape(6.dp))
                    .padding(horizontal = 6.dp, vertical = 2.dp),
            )
        }
    }
}

/** Shows the real photo through Coil when there is one, otherwise the striped placeholder. */
@Composable
fun DealImage(
    imageUrl: String?,
    label: String?,
    modifier: Modifier = Modifier,
    shape: Shape = RectangleShape,
    content: @Composable BoxScope.() -> Unit = {},
) {
    Box(modifier.clip(shape)) {
        if (imageUrl != null) {
            AsyncImage(
                model = imageUrl,
                contentDescription = label,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        } else {
            PlaceholderImage(label = label, modifier = Modifier.fillMaxSize())
        }
        content()
    }
}

/** Small square thumbnail used in lists (orders, favorites, checkout). */
@Composable
fun Thumbnail(imageUrl: String? = null, modifier: Modifier = Modifier, size: Int = 52) {
    DealImage(imageUrl = imageUrl, label = null, modifier = modifier.size(size.dp), shape = RoundedCornerShape(12.dp))
}

@Preview
@Composable
private fun PlaceholderPreview() {
    FreshSaveTheme { PlaceholderImage("bakery bag photo", Modifier.size(220.dp, 140.dp)) }
}
