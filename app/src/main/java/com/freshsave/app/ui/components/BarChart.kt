package com.freshsave.app.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.freshsave.app.ui.theme.FreshSaveTheme

/**
 * Simple vertical bar chart drawn on a Canvas. Bars get darker green from left to right
 * (older -> newer), the last one is the solid brand green, and they grow in when shown.
 */
@Composable
fun BarChart(
    values: List<Int>,
    labels: List<String>,
    modifier: Modifier = Modifier,
    chartHeight: Dp = 120.dp,
    barSpacing: Dp = 10.dp,
    description: String = "Bar chart",
) {
    val colors = FreshSaveTheme.colors
    val max = (values.maxOrNull() ?: 0).coerceAtLeast(1)
    // 0 -> 1 animation progress; restarts if the data changes.
    val progress = remember(values) { Animatable(0f) }
    LaunchedEffect(values) { progress.animateTo(1f, tween(800, easing = FastOutSlowInEasing)) }

    val lightest = lerp(colors.greenTint, colors.card, 0.2f)
    Column(modifier.semantics { contentDescription = description }) {
        Canvas(
            Modifier
                .fillMaxWidth()
                .height(chartHeight),
        ) {
            val gap = barSpacing.toPx()
            val n = values.size.coerceAtLeast(1)
            val barWidth = (size.width - gap * (n - 1)) / n
            values.forEachIndexed { i, v ->
                val t = if (n == 1) 1f else i / (n - 1f)
                val barColor = if (i == n - 1) colors.green else lerp(lightest, colors.green, 0.15f + 0.65f * t)
                val h = size.height * (v.toFloat() / max) * progress.value
                drawRoundRect(
                    color = barColor,
                    topLeft = Offset(i * (barWidth + gap), size.height - h),
                    size = Size(barWidth, h),
                    cornerRadius = CornerRadius(6.dp.toPx()),
                )
            }
        }
        Spacer(Modifier.height(8.dp))
        Row(Modifier.fillMaxWidth()) {
            labels.forEachIndexed { i, label ->
                val last = i == labels.lastIndex
                Text(
                    label,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = if (last) FontWeight.Bold else FontWeight.Medium,
                    color = if (last) colors.green else colors.textMuted,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 1.dp),
                )
            }
        }
    }
}

@Preview(widthDp = 320)
@Composable
private fun BarChartPreview() {
    FreshSaveTheme {
        BarChart(listOf(420, 760, 640, 1050, 1410), listOf("Jun", "Jul", "Aug", "Sep", "Oct"), Modifier.padding(16.dp))
    }
}
