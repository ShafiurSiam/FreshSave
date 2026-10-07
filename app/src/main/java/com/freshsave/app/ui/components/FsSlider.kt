package com.freshsave.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Slider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.freshsave.app.ui.theme.FreshSaveTheme

/**
 * Material's Slider with a thin rounded track and a white round thumb, matching the
 * distance and discount sliders in App.pdf. Behaviour (drag, steps, accessibility) is Material's.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FsSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    valueRange: ClosedFloatingPointRange<Float> = 0f..1f,
    steps: Int = 0,
    activeColor: Color = FreshSaveTheme.colors.green,
) {
    val inactive = FreshSaveTheme.colors.surfaceMuted
    val interaction = remember { MutableInteractionSource() }
    Slider(
        value = value,
        onValueChange = onValueChange,
        valueRange = valueRange,
        steps = steps,
        interactionSource = interaction,
        modifier = modifier,
        thumb = {
            Box(
                Modifier
                    .size(24.dp)
                    .shadow(3.dp, CircleShape)
                    .background(Color.White, CircleShape)
                    .border(3.dp, activeColor, CircleShape),
            )
        },
        track = { state ->
            val span = state.valueRange.endInclusive - state.valueRange.start
            val fraction = if (span == 0f) 0f else (state.value - state.valueRange.start) / span
            Canvas(
                Modifier
                    .fillMaxWidth()
                    .height(6.dp),
            ) {
                val y = size.height / 2
                val stroke = size.height
                drawLine(inactive, Offset(0f, y), Offset(size.width, y), stroke, StrokeCap.Round)
                drawLine(activeColor, Offset(0f, y), Offset(size.width * fraction, y), stroke, StrokeCap.Round)
            }
        },
    )
}

@Preview(widthDp = 320)
@Composable
private fun FsSliderPreview() {
    FreshSaveTheme {
        Box(Modifier.padding(16.dp)) { FsSlider(0.4f, {}) }
    }
}
