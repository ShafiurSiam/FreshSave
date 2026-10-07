package com.freshsave.app.feature.onboarding

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.freshsave.app.data.repository.SessionRepository
import com.freshsave.app.ui.appViewModel
import com.freshsave.app.ui.components.PlaceholderImage
import com.freshsave.app.ui.theme.FreshSaveTheme
import kotlinx.coroutines.launch

data class OnboardingSlide(val sticker: String, val title: String, val body: String, val imageLabel: String)

val onboardingSlides = listOf(
    OnboardingSlide(
        sticker = "1.3B tonnes\nwasted / year",
        title = "A third of all food\nnever gets eaten",
        body = "Perfectly good meals from cafés & bakeries near you get binned every night. Let's change that.",
        imageLabel = "illustration · food waste",
    ),
    OnboardingSlide(
        sticker = "Up to\n70% off",
        title = "Rescue tonight's\nsurplus near you",
        body = "Shops in Gulshan, Banani & Dhanmondi post their leftovers at a big discount before closing.",
        imageLabel = "illustration · shop counter",
    ),
    OnboardingSlide(
        sticker = "Pick up in\nminutes",
        title = "Reserve, pay,\npick up",
        body = "Pay with bKash or Nagad, show your pickup code at the counter, and enjoy. No printing needed.",
        imageLabel = "illustration · pickup code",
    ),
)

class OnboardingViewModel(private val sessionRepository: SessionRepository) : ViewModel() {
    /** Remember that the intro was seen so the splash skips it next launch. */
    fun finish() {
        viewModelScope.launch { sessionRepository.setOnboardingSeen() }
    }
}

@Composable
fun OnboardingRoute(onDone: () -> Unit) {
    val viewModel = appViewModel { OnboardingViewModel(it.sessionRepository) }
    OnboardingScreen(onDone = {
        viewModel.finish()
        onDone()
    })
}

@Composable
fun OnboardingScreen(onDone: () -> Unit) {
    val colors = FreshSaveTheme.colors
    // The pager remembers which slide is showing and animates swipes between them.
    val pagerState = rememberPagerState { onboardingSlides.size }
    val scope = rememberCoroutineScope()
    val isLast = pagerState.currentPage == onboardingSlides.lastIndex

    Column(
        Modifier
            .fillMaxSize()
            .background(colors.background)
            .systemBarsPadding(),
    ) {
        Box(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp)) {
            Text(
                "Skip",
                style = MaterialTheme.typography.titleSmall,
                color = colors.textMuted,
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .clip(RoundedCornerShape(8.dp))
                    .clickable(onClick = onDone)
                    .padding(horizontal = 12.dp, vertical = 8.dp),
            )
        }

        HorizontalPager(state = pagerState, modifier = Modifier.weight(1f)) { page ->
            SlideContent(onboardingSlides[page])
        }

        Row(
            Modifier
                .fillMaxWidth()
                .padding(start = 28.dp, end = 24.dp, bottom = 28.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                repeat(onboardingSlides.size) { index ->
                    val selected = index == pagerState.currentPage
                    val width by animateDpAsState(if (selected) 26.dp else 8.dp, label = "dot-width")
                    val color by animateColorAsState(if (selected) colors.green else colors.outline, label = "dot-color")
                    Box(
                        Modifier
                            .height(8.dp)
                            .width(width)
                            .background(color, CircleShape),
                    )
                }
            }
            FloatingActionButton(
                onClick = {
                    if (isLast) onDone() else scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) }
                },
                shape = CircleShape,
                containerColor = colors.green,
                contentColor = Color.White,
                elevation = FloatingActionButtonDefaults.elevation(6.dp),
                modifier = Modifier.size(64.dp),
            ) {
                Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = if (isLast) "Get started" else "Next")
            }
        }
    }
}

@Composable
private fun SlideContent(slide: OnboardingSlide) {
    val colors = FreshSaveTheme.colors
    Column(
        Modifier
            .fillMaxSize()
            .padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(Modifier.size(width = 290.dp, height = 270.dp)) {
            PlaceholderImage(
                label = slide.imageLabel,
                labelAlignment = Alignment.Center,
                shape = RoundedCornerShape(32.dp),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .size(250.dp),
            )
            // Tilted orange stat "sticker" overlapping the illustration's corner.
            Text(
                slide.sticker,
                style = MaterialTheme.typography.titleMedium,
                color = Color.White,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = (-4).dp, y = 8.dp)
                    .rotate(4f)
                    .shadow(12.dp, RoundedCornerShape(14.dp), ambientColor = colors.orange, spotColor = colors.orange)
                    .background(colors.orange, RoundedCornerShape(14.dp))
                    .padding(horizontal = 16.dp, vertical = 10.dp),
            )
        }
        Spacer(Modifier.height(36.dp))
        Text(
            slide.title,
            style = MaterialTheme.typography.headlineMedium,
            color = colors.textPrimary,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(14.dp))
        Text(
            slide.body,
            style = MaterialTheme.typography.bodyLarge,
            color = colors.textMuted,
            textAlign = TextAlign.Center,
        )
    }
}

@Preview(widthDp = 360, heightDp = 760)
@Composable
private fun OnboardingPreview() {
    FreshSaveTheme { OnboardingScreen(onDone = {}) }
}
