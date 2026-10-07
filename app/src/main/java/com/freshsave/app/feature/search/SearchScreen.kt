package com.freshsave.app.feature.search

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.SearchOff
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.freshsave.app.data.fake.MockData
import com.freshsave.app.data.model.FoodCategory
import com.freshsave.app.ui.appViewModel
import com.freshsave.app.ui.components.DealCard
import com.freshsave.app.ui.components.EmptyState
import com.freshsave.app.ui.components.FilterChip
import com.freshsave.app.ui.components.FsSlider
import com.freshsave.app.ui.components.FsTextField
import com.freshsave.app.ui.components.PrimaryButton
import com.freshsave.app.ui.components.SecondaryButton
import com.freshsave.app.ui.theme.FreshSaveTheme
import java.util.Locale

@Composable
fun SearchRoute(onCancel: () -> Unit, onDealClick: (String) -> Unit) {
    val viewModel = appViewModel { SearchViewModel(it.clock, it.dealRepository) }
    val state by viewModel.state.collectAsStateWithLifecycle()
    // System back from the results list returns to the filters first.
    BackHandler(enabled = state.showingResults) { viewModel.editFilters() }
    SearchScreen(
        state = state,
        actions = SearchActions(
            onQueryChange = viewModel::onQueryChange,
            onCancel = {
                viewModel.reset()
                onCancel()
            },
            onDistanceChange = viewModel::onDistanceChange,
            onPriceSelected = viewModel::onPriceSelected,
            onCategoryToggled = viewModel::onCategoryToggled,
            onPickupTimeSelected = viewModel::onPickupTimeSelected,
            onReset = viewModel::reset,
            onShowResults = viewModel::showResults,
            onEditFilters = viewModel::editFilters,
            onDealClick = onDealClick,
        ),
    )
}

/** Bundles the callbacks so the screen signature stays readable. */
data class SearchActions(
    val onQueryChange: (String) -> Unit = {},
    val onCancel: () -> Unit = {},
    val onDistanceChange: (Float) -> Unit = {},
    val onPriceSelected: (PriceRange) -> Unit = {},
    val onCategoryToggled: (FoodCategory) -> Unit = {},
    val onPickupTimeSelected: (PickupTime) -> Unit = {},
    val onReset: () -> Unit = {},
    val onShowResults: () -> Unit = {},
    val onEditFilters: () -> Unit = {},
    val onDealClick: (String) -> Unit = {},
)

@Composable
fun SearchScreen(state: SearchUiState, actions: SearchActions) {
    val colors = FreshSaveTheme.colors
    val focusManager = LocalFocusManager.current
    Column(
        Modifier
            .fillMaxSize()
            .background(colors.background)
            .statusBarsPadding()
            .imePadding(),
    ) {
        Row(
            Modifier.padding(start = 20.dp, end = 12.dp, top = 12.dp, bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            FsTextField(
                value = state.filters.query,
                onValueChange = actions.onQueryChange,
                placeholder = "Search deals or shops",
                leadingIcon = { Icon(Icons.Outlined.Search, null, tint = colors.textMuted) },
                trailing = if (state.filters.query.isNotEmpty()) {
                    {
                        IconButton(onClick = { actions.onQueryChange("") }) {
                            Icon(Icons.Filled.Close, "Clear", tint = colors.textMuted)
                        }
                    }
                } else null,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = {
                    focusManager.clearFocus()
                    actions.onShowResults()
                }),
                modifier = Modifier.weight(1f),
            )
            Text(
                "Cancel",
                style = MaterialTheme.typography.titleSmall,
                color = colors.green,
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable(onClick = actions.onCancel)
                    .padding(horizontal = 8.dp, vertical = 10.dp),
            )
        }

        AnimatedContent(targetState = state.showingResults, label = "search-mode", modifier = Modifier.weight(1f)) { showing ->
            if (showing) ResultsList(state, actions) else FiltersPanel(state, actions)
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FiltersPanel(state: SearchUiState, actions: SearchActions) {
    val colors = FreshSaveTheme.colors
    val f = state.filters
    Column(Modifier.fillMaxSize()) {
        Column(
            Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
        ) {
            Spacer(Modifier.height(16.dp))
            Text("Filters", style = MaterialTheme.typography.titleLarge, color = colors.textPrimary)
            Spacer(Modifier.height(18.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                FilterLabel("Distance", Modifier.weight(1f))
                Text(
                    if (f.maxDistanceKm >= SearchFilters.MAX_DISTANCE_KM) "any distance"
                    else "within ${String.format(Locale.US, "%.1f", f.maxDistanceKm).removeSuffix(".0")} km",
                    style = MaterialTheme.typography.titleSmall,
                    color = colors.green,
                )
            }
            FsSlider(
                value = f.maxDistanceKm,
                onValueChange = actions.onDistanceChange,
                valueRange = 0.5f..SearchFilters.MAX_DISTANCE_KM,
                steps = 8, // 0.5 km increments
            )

            Spacer(Modifier.height(14.dp))
            FilterLabel("Price")
            ChipRow {
                PriceRange.entries.forEach { range ->
                    FilterChip(range.label, f.price == range, { actions.onPriceSelected(range) })
                }
            }

            Spacer(Modifier.height(20.dp))
            FilterLabel("Food type")
            ChipRow {
                FoodCategory.entries.forEach { category ->
                    FilterChip(category.label, category in f.categories, { actions.onCategoryToggled(category) })
                }
            }

            Spacer(Modifier.height(20.dp))
            FilterLabel("Pickup time")
            ChipRow {
                PickupTime.entries.forEach { time ->
                    FilterChip(time.label, f.pickupTime == time, { actions.onPickupTimeSelected(time) })
                }
            }
            Spacer(Modifier.height(24.dp))
        }
        Row(
            Modifier.padding(horizontal = 20.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            SecondaryButton("Reset", actions.onReset, Modifier.weight(0.8f))
            PrimaryButton(
                text = when {
                    state.loading -> "Loading deals…"
                    state.results.isEmpty() -> "No deals match"
                    state.results.size == 1 -> "Show 1 deal"
                    else -> "Show ${state.results.size} deals"
                },
                onClick = actions.onShowResults,
                enabled = state.results.isNotEmpty(),
                modifier = Modifier.weight(1.5f),
            )
        }
    }
}

@Composable
private fun ResultsList(state: SearchUiState, actions: SearchActions) {
    val colors = FreshSaveTheme.colors
    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "${state.results.size} deal${if (state.results.size == 1) "" else "s"} found",
                    style = MaterialTheme.typography.titleLarge,
                    color = colors.textPrimary,
                    modifier = Modifier.weight(1f),
                )
                Row(
                    Modifier
                        .clip(RoundedCornerShape(50))
                        .background(colors.greenTint)
                        .clickable(onClick = actions.onEditFilters)
                        .padding(horizontal = 12.dp, vertical = 7.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.Outlined.Tune, null, tint = colors.green, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.size(4.dp))
                    Text("Filters", style = MaterialTheme.typography.labelLarge, color = colors.green)
                }
            }
        }
        if (state.results.isEmpty()) {
            item {
                EmptyState(
                    "No deals match",
                    "Try a wider distance or fewer filters.",
                    icon = Icons.Outlined.SearchOff,
                    actionLabel = "Reset filters",
                    onAction = actions.onReset,
                )
            }
        }
        items(state.results, key = { it.id }) { deal ->
            DealCard(deal, onClick = { actions.onDealClick(deal.id) }, modifier = Modifier.animateItem())
        }
    }
}

@Composable
private fun FilterLabel(text: String, modifier: Modifier = Modifier) {
    Text(text, style = MaterialTheme.typography.titleSmall, color = FreshSaveTheme.colors.textPrimary, modifier = modifier)
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ChipRow(content: @Composable () -> Unit) {
    // FlowRow wraps chips onto the next line when they don't fit, like text.
    FlowRow(
        Modifier
            .fillMaxWidth()
            .padding(top = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) { content() }
}

@Preview(widthDp = 380, heightDp = 800)
@Composable
private fun SearchPreview() {
    val deals = MockData.deals(System.currentTimeMillis())
    FreshSaveTheme {
        SearchScreen(
            SearchUiState(
                filters = SearchFilters(query = "croissant", maxDistanceKm = 2f, price = PriceRange.UNDER_100, categories = setOf(FoodCategory.BAKERY)),
                results = deals.take(3),
            ),
            SearchActions(),
        )
    }
}
