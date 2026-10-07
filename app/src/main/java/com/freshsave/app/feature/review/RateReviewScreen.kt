package com.freshsave.app.feature.review

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.freshsave.app.data.fake.MockData
import com.freshsave.app.data.model.Order
import com.freshsave.app.data.repository.OrderRepository
import com.freshsave.app.data.repository.ReviewRepository
import com.freshsave.app.navigation.LocalSnackbarHostState
import com.freshsave.app.ui.appViewModel
import com.freshsave.app.ui.components.FilterChip
import com.freshsave.app.ui.components.FsTextField
import com.freshsave.app.ui.components.FsTopBar
import com.freshsave.app.ui.components.LocalNow
import com.freshsave.app.ui.components.NavIcon
import com.freshsave.app.ui.components.PlaceholderImage
import com.freshsave.app.ui.components.PrimaryButton
import com.freshsave.app.ui.components.StarRow
import com.freshsave.app.ui.theme.FreshSaveTheme
import com.freshsave.app.util.formatDayWord
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

val feedbackTags = listOf("Fresh", "Generous portion", "Easy pickup", "Friendly staff", "Great value", "Well packed")

fun ratingLabel(stars: Int) = when (stars) {
    1 -> "Not great"
    2 -> "Could be better"
    3 -> "It was okay"
    4 -> "Great value!"
    5 -> "Amazing rescue!"
    else -> "Tap a star to rate"
}

data class ReviewForm(
    val rating: Int = 0,
    val tags: Set<String> = emptySet(),
    val note: String = "",
    val submitting: Boolean = false,
    val submitted: Boolean = false,
)

data class RateReviewUiState(val order: Order? = null, val form: ReviewForm = ReviewForm())

class RateReviewViewModel(
    private val orderId: String,
    private val orderRepository: OrderRepository,
    private val reviewRepository: ReviewRepository,
) : ViewModel() {
    private val form = MutableStateFlow(ReviewForm())

    val state: StateFlow<RateReviewUiState> = combine(orderRepository.observeOrder(orderId), form) { order, f ->
        RateReviewUiState(order, f)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), RateReviewUiState())

    fun setRating(stars: Int) = form.update { it.copy(rating = stars) }
    fun toggleTag(tag: String) = form.update { it.copy(tags = if (tag in it.tags) it.tags - tag else it.tags + tag) }
    fun setNote(note: String) = form.update { it.copy(note = note.take(280)) }

    fun submit() {
        val order = state.value.order ?: return
        val f = form.value
        if (f.rating == 0 || f.submitting) return
        viewModelScope.launch {
            form.update { it.copy(submitting = true) }
            reviewRepository.submitReview(order.shopId, f.rating, f.tags.toList(), f.note)
            orderRepository.markReviewed(orderId)
            form.update { it.copy(submitting = false, submitted = true) }
        }
    }
}

@Composable
fun RateReviewRoute(orderId: String, onClose: () -> Unit) {
    val viewModel = appViewModel(key = "review-$orderId") {
        RateReviewViewModel(orderId, it.orderRepository, it.reviewRepository)
    }
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbar = LocalSnackbarHostState.current
    LaunchedEffect(state.form.submitted) {
        if (state.form.submitted) {
            onClose()
            snackbar.showSnackbar("Thanks! Your review helps other rescuers.")
        }
    }
    RateReviewScreen(state, onClose, viewModel::setRating, viewModel::toggleTag, viewModel::setNote, viewModel::submit)
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun RateReviewScreen(
    state: RateReviewUiState,
    onClose: () -> Unit,
    onRate: (Int) -> Unit,
    onToggleTag: (String) -> Unit,
    onNoteChange: (String) -> Unit,
    onSubmit: () -> Unit,
) {
    val colors = FreshSaveTheme.colors
    val order = state.order
    val form = state.form
    val now = LocalNow.current
    Column(
        Modifier
            .fillMaxSize()
            .background(colors.background)
            .systemBarsPadding()
            .imePadding(),
    ) {
        FsTopBar("Rate your pickup", navIcon = NavIcon.Close, onNavClick = onClose)
        Column(
            Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.height(16.dp))
            PlaceholderImage(null, Modifier.size(84.dp), shape = RoundedCornerShape(20.dp))
            Spacer(Modifier.height(14.dp))
            if (order != null) {
                Text("${order.shopName} · ${order.area}", style = MaterialTheme.typography.titleLarge, color = colors.textPrimary)
                Text(
                    "${order.itemTitle} · picked up ${formatDayWord(order.collectedAtMillis ?: order.pickupEndMillis, now)}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.textMuted,
                    textAlign = TextAlign.Center,
                )
            }
            Spacer(Modifier.height(20.dp))
            StarRow(rating = form.rating, starSize = 40.dp, spacing = 8.dp, onRate = onRate)
            Spacer(Modifier.height(8.dp))
            AnimatedContent(form.rating, label = "rating-label") { r ->
                Text(
                    ratingLabel(r),
                    style = MaterialTheme.typography.titleSmall,
                    color = if (r == 0) colors.textMuted else colors.green,
                )
            }
            Spacer(Modifier.height(26.dp))
            Text(
                "What stood out?",
                style = MaterialTheme.typography.titleMedium,
                color = colors.textPrimary,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(10.dp))
            FlowRow(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                feedbackTags.forEach { tag -> FilterChip(tag, tag in form.tags, { onToggleTag(tag) }) }
            }
            Spacer(Modifier.height(18.dp))
            FsTextField(
                value = form.note,
                onValueChange = onNoteChange,
                placeholder = "Add a note for other rescuers… (optional)",
                singleLine = false,
                minLines = 3,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(20.dp))
        }
        PrimaryButton(
            text = "Submit review",
            onClick = onSubmit,
            enabled = form.rating > 0 && order != null,
            loading = form.submitting,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 14.dp),
        )
    }
}

@Preview(widthDp = 380, heightDp = 800)
@Composable
private fun RateReviewPreview() {
    FreshSaveTheme {
        RateReviewScreen(
            RateReviewUiState(
                MockData.orders(System.currentTimeMillis())[2],
                ReviewForm(rating = 4, tags = setOf("Fresh", "Generous portion")),
            ),
            {}, {}, {}, {}, {},
        )
    }
}
