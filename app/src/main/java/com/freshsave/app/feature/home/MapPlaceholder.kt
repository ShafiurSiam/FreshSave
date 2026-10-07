package com.freshsave.app.feature.home

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.freshsave.app.ui.theme.FreshSaveTheme
import com.freshsave.app.util.formatTaka

/**
 * A drawn stand-in for a real map: soft green blocks, a few roads and price pins.
 * TODO: Replace with Google Maps Compose (com.google.maps.android:maps-compose) — a GoogleMap()
 *  with one MarkerComposable per pin — once a Maps API key is configured.
 */
@Composable
fun MapPlaceholder(
    pins: List<MapPin>,
    onPinClick: (MapPin) -> Unit,
    modifier: Modifier = Modifier,
    pinScale: Float = 1f,
) {
    val colors = FreshSaveTheme.colors
    val land = if (colors.isDark) Color(0xFF1C2A22) else Color(0xFFE4EEE3)
    val block = if (colors.isDark) Color(0xFF22322A) else Color(0xFFD8E7D6)
    val road = if (colors.isDark) Color(0xFF2E3B33) else Color(0xFFF4EFE4)
    BoxWithConstraints(modifier.background(land)) {
        Canvas(Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            // Park-like blocks.
            drawRect(block, Offset(w * 0.05f, h * 0.08f), Size(w * 0.22f, h * 0.35f))
            drawRect(block, Offset(w * 0.55f, h * 0.70f), Size(w * 0.30f, h * 0.25f))
            drawRect(block, Offset(w * 0.68f, h * 0.05f), Size(w * 0.18f, h * 0.22f))
            // Roads: two horizontals, two verticals and a diagonal avenue.
            val thick = 14.dp.toPx()
            val thin = 8.dp.toPx()
            drawLine(road, Offset(0f, h * 0.55f), Offset(w, h * 0.48f), thick)
            drawLine(road, Offset(0f, h * 0.18f), Offset(w, h * 0.30f), thin)
            drawLine(road, Offset(w * 0.40f, 0f), Offset(w * 0.36f, h), thick)
            drawLine(road, Offset(w * 0.12f, h * 0.55f), Offset(w * 0.12f, h), thin * 2)
            drawLine(road, Offset(w * 0.60f, h), Offset(w, h * 0.20f), thin)
        }
        val width = maxWidth
        val height = maxHeight
        pins.forEach { pin ->
            val bg = if (pin.endingSoonest) colors.orange else colors.green
            Text(
                formatTaka(pin.price),
                style = MaterialTheme.typography.labelLarge.copy(fontSize = MaterialTheme.typography.labelLarge.fontSize * pinScale),
                color = Color.White,
                modifier = Modifier
                    .offset(x = width * pin.x - 28.dp, y = height * pin.y - 14.dp)
                    .shadow(4.dp, RoundedCornerShape(10.dp))
                    .clip(RoundedCornerShape(10.dp))
                    .background(bg)
                    .clickable { onPinClick(pin) }
                    .padding(horizontal = 10.dp * pinScale, vertical = 5.dp * pinScale),
            )
        }
    }
}

@Preview
@Composable
private fun MapPreview() {
    FreshSaveTheme {
        Box(Modifier.size(320.dp, 150.dp)) {
            MapPlaceholder(
                pins = listOf(
                    MapPin("a", 75, 0.3f, 0.4f, false),
                    MapPin("b", 90, 0.62f, 0.62f, true),
                    MapPin("c", 150, 0.86f, 0.3f, false),
                ),
                onPinClick = {},
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}
