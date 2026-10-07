package com.freshsave.app.feature.business.ratings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Reply
import androidx.compose.material.icons.outlined.RateReview
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.freshsave.app.data.fake.MockData
import com.freshsave.app.data.model.RatingSummary
import com.freshsave.app.data.model.Review
import com.freshsave.app.data.repository.BusinessRepository
import com.freshsave.app.data.repository.ReviewRepository
import com.freshsave.app.ui.appViewModel
import com.freshsave.app.ui.components.Avatar
import com.freshsave.app.ui.components.EmptyState
import com.freshsave.app.ui.components.FsCard
import com.freshsave.app.ui.components.FsTextField
import com.freshsave.app.ui.components.FsTopBar
import com.freshsave.app.ui.components.StarRow
import com.freshsave.app.ui.components.avatarColorFor
import com.freshsave.app.ui.theme.FreshSaveTheme
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

data class RatingsUiState(
    val loading: Boolean = true,
    val summary: RatingSummary = RatingSummary(0.0, 0, List(5) { 0 }),
    val reviews: List<Review> = emptyList(),
)

class RatingsViewModel(businessRepository: BusinessRepository, private val reviewRepository: ReviewRepository) : ViewModel() {
    private val shopId = businessRepository.currentShopId

    val state: StateFlow<RatingsUiState> = combine(
        reviewRepository.observeRatingSummary(shopId),
        reviewRepository.observeShopReviews(shopId),
    ) { summary, reviews -> RatingsUiState(false, summary, reviews) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), RatingsUiState())

    fun reply(reviewId: String, text: String) {
        viewModelScope.launch { reviewRepository.reply(reviewId, text) }
    }
}

@Composable
fun RatingsRoute(onBack: () -> Unit) {
    val viewModel = appViewModel { RatingsViewModel(it.businessRepository, it.reviewRepository) }
    val state by viewModel.state.collectAsStateWithLifecycle()
    var replyingTo by remember { mutableStateOf<Review?>(null) }
    RatingsScreen(state, onBack, onReply = { replyingTo = it })
    replyingTo?.let { review ->
        ReplyDialog(
            review = review,
            onDismiss = { replyingTo = null },
            onSend = { text ->
                viewModel.reply(review.id, text)
                replyingTo = null
            },
        )
    }
}

@Composable
fun RatingsScreen(state: RatingsUiState, onBack: () -> Unit, onReply: (Review) -> Unit) {
    val colors = FreshSaveTheme.colors
    Column(
        Modifier
            .fillMaxSize()
            .background(colors.background)
            .statusBarsPadding(),
    ) {
        FsTopBar("Your ratings", onNavClick = onBack)
        LazyColumn(
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.navigationBarsPadding(),
        ) {
            item { SummaryCard(state.summary) }
            if (!state.loading && state.reviews.isEmpty()) {
                item { EmptyState("No reviews yet", "Reviews from shoppers will appear here.", icon = Icons.Outlined.RateReview) }
            }
            items(state.reviews, key = { it.id }) { review ->
                ReviewCard(review, onReply = { onReply(review) }, modifier = Modifier.animateItem())
            }
        }
    }
}

@Composable
private fun SummaryCard(summary: RatingSummary) {
    val colors = FreshSaveTheme.colors
    // Bar colours from 5★ (green) down to 1★ (red), like the mock-up.
    val barColors = listOf(colors.danger, colors.orange, Color(0xFFE8B931), colors.green, colors.green)
    FsCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(end = 20.dp)) {
                Text(
                    "%.1f".format(summary.average),
                    style = MaterialTheme.typography.displaySmall.copy(fontSize = 42.sp),
                    color = colors.textPrimary,
                )
                StarRow(summary.average.roundToInt(), starSize = 14.dp)
                Text("${summary.total} reviews", style = MaterialTheme.typography.bodySmall, color = colors.textMuted)
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                for (stars in 5 downTo 1) {
                    val count = summary.distribution.getOrElse(stars - 1) { 0 }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("$stars", style = MaterialTheme.typography.labelSmall, color = colors.textMuted, modifier = Modifier.width(14.dp))
                        LinearProgressIndicator(
                            progress = { if (summary.total == 0) 0f else count / summary.total.toFloat() },
                            color = barColors[stars - 1],
                            trackColor = colors.surfaceMuted,
                            strokeCap = StrokeCap.Round,
                            gapSize = 0.dp,
                            drawStopIndicator = {},
                            modifier = Modifier.weight(1f).height(6.dp),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ReviewCard(review: Review, onReply: () -> Unit, modifier: Modifier = Modifier) {
    val colors = FreshSaveTheme.colors
    FsCard(modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Avatar(review.authorName.take(1), color = avatarColorFor(review.authorName), size = 32.dp)
            Spacer(Modifier.width(10.dp))
            Text(review.authorName, style = MaterialTheme.typography.titleSmall, color = colors.textPrimary, modifier = Modifier.weight(1f))
            StarRow(review.rating, starSize = 15.dp)
        }
        if (review.text.isNotBlank()) {
            Spacer(Modifier.height(10.dp))
            Text("“${review.text}”", style = MaterialTheme.typography.bodyMedium, color = colors.textMuted)
        }
        if (review.tags.isNotEmpty()) {
            Spacer(Modifier.height(6.dp))
            Text(review.tags.joinToString(" · "), style = MaterialTheme.typography.labelMedium, color = colors.green)
        }
        Spacer(Modifier.height(10.dp))
        if (review.reply != null) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .background(colors.greenTint, RoundedCornerShape(12.dp))
                    .padding(12.dp),
            ) {
                Text("Your reply", style = MaterialTheme.typography.labelMedium, color = colors.green)
                Text(review.reply, style = MaterialTheme.typography.bodyMedium, color = colors.textPrimary)
            }
        } else {
            Row(
                Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable(onClick = onReply)
                    .padding(vertical = 4.dp, horizontal = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.AutoMirrored.Filled.Reply, null, tint = colors.green, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text("Reply", style = MaterialTheme.typography.labelLarge, color = colors.green)
            }
        }
    }
}

@Composable
private fun ReplyDialog(review: Review, onDismiss: () -> Unit, onSend: (String) -> Unit) {
    var text by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = FreshSaveTheme.colors.card,
        title = { Text("Reply to ${review.authorName}") },
        text = {
            FsTextField(
                value = text,
                onValueChange = { text = it.take(300) },
                placeholder = "Thanks for rescuing with us!",
                singleLine = false,
                minLines = 3,
            )
        },
        confirmButton = { TextButton(onClick = { onSend(text) }, enabled = text.isNotBlank()) { Text("Post reply") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

@Preview(widthDp = 380, heightDp = 800)
@Composable
private fun RatingsPreview() {
    FreshSaveTheme {
        RatingsScreen(
            RatingsUiState(false, RatingSummary(4.8, 312, MockData.tastyTreatDistribution), MockData.reviews(System.currentTimeMillis())),
            {}, {},
        )
    }
}
