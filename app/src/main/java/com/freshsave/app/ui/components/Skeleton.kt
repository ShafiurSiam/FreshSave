package com.freshsave.app.ui.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.freshsave.app.ui.theme.FreshSaveTheme

/** Grey block that gently pulses; stands in for content that is still loading. */
@Composable
fun SkeletonBlock(modifier: Modifier = Modifier) {
    // rememberInfiniteTransition runs forever while this composable is on screen.
    val pulse by rememberInfiniteTransition(label = "skeleton").animateFloat(
        initialValue = 0.45f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(750), RepeatMode.Reverse),
        label = "skeleton-alpha",
    )
    Box(
        modifier
            .alpha(pulse)
            .clip(RoundedCornerShape(8.dp))
            .background(FreshSaveTheme.colors.surfaceMuted),
    )
}

/** Same shape as [DealCard], shown while deals load. */
@Composable
fun SkeletonDealCard(modifier: Modifier = Modifier) {
    val colors = FreshSaveTheme.colors
    Column(
        modifier
            .fillMaxWidth()
            .clip(CardShape)
            .background(colors.card)
            .border(1.dp, colors.outline, CardShape)
            .semantics { contentDescription = "Loading deal" },
    ) {
        SkeletonBlock(Modifier.fillMaxWidth().height(132.dp))
        Column(Modifier.padding(16.dp)) {
            SkeletonBlock(Modifier.fillMaxWidth(0.5f).height(18.dp))
            Spacer(Modifier.height(8.dp))
            SkeletonBlock(Modifier.fillMaxWidth(0.75f).height(14.dp))
            Spacer(Modifier.height(10.dp))
            SkeletonBlock(Modifier.fillMaxWidth(0.3f).height(22.dp))
        }
    }
}

@Preview(widthDp = 360)
@Composable
private fun SkeletonPreview() {
    FreshSaveTheme { SkeletonDealCard(Modifier.padding(16.dp)) }
}
