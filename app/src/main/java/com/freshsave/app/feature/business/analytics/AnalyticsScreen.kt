package com.freshsave.app.feature.business.analytics

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.freshsave.app.data.fake.MockData
import com.freshsave.app.data.model.AnalyticsSummary
import com.freshsave.app.data.repository.BusinessRepository
import com.freshsave.app.ui.appViewModel
import com.freshsave.app.ui.components.BarChart
import com.freshsave.app.ui.components.FsCard
import com.freshsave.app.ui.components.ScreenTitle
import com.freshsave.app.ui.components.StatusPill
import com.freshsave.app.ui.theme.FreshSaveTheme
import com.freshsave.app.util.formatTaka
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

class AnalyticsViewModel(businessRepository: BusinessRepository) : ViewModel() {
    val state: StateFlow<AnalyticsSummary?> = businessRepository.observeAnalytics()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
}

@Composable
fun AnalyticsRoute() {
    val viewModel = appViewModel { AnalyticsViewModel(it.businessRepository) }
    val summary by viewModel.state.collectAsStateWithLifecycle()
    AnalyticsScreen(summary)
}

@Composable
fun AnalyticsScreen(summary: AnalyticsSummary?) {
    val colors = FreshSaveTheme.colors
    Column(
        Modifier
            .fillMaxSize()
            .background(colors.background)
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        ScreenTitle("Analytics") { StatusPill("★ PRO PLAN") }
        if (summary == null) {
            CircularProgressIndicator(color = colors.green, modifier = Modifier.align(Alignment.CenterHorizontally))
            return@Column
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            KpiTile("Revenue (30d)", formatTaka(summary.revenue30d), summary.revenueChangePct, Modifier.weight(1f))
            KpiTile("Waste cut", "${summary.wasteCutKg} kg", summary.wasteChangePct, Modifier.weight(1f))
        }
        FsCard {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Revenue recovered", style = MaterialTheme.typography.titleMedium, color = colors.textPrimary, modifier = Modifier.weight(1f))
                Text("last 8 weeks", style = MaterialTheme.typography.bodySmall, color = colors.textMuted)
            }
            Spacer(Modifier.height(16.dp))
            BarChart(
                values = summary.weeklyRevenue,
                labels = summary.weeklyRevenue.indices.map { "W${it + 1}" },
                chartHeight = 140.dp,
                barSpacing = 8.dp,
                description = "Weekly revenue recovered: " + summary.weeklyRevenue.joinToString { formatTaka(it) },
            )
        }
        FsCard {
            Text("Top-selling surplus", style = MaterialTheme.typography.titleMedium, color = colors.textPrimary)
            Spacer(Modifier.height(12.dp))
            val max = summary.topSellers.maxOfOrNull { it.sold }?.coerceAtLeast(1) ?: 1
            summary.topSellers.forEachIndexed { index, seller ->
                Row(Modifier.padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(seller.name, style = MaterialTheme.typography.bodyMedium, color = colors.textPrimary, modifier = Modifier.width(110.dp))
                    LinearProgressIndicator(
                        progress = { seller.sold / max.toFloat() },
                        color = when (index) {
                            0 -> colors.green
                            1 -> colors.green.copy(alpha = 0.7f)
                            else -> colors.orange
                        },
                        trackColor = colors.surfaceMuted,
                        strokeCap = StrokeCap.Round,
                        gapSize = 0.dp,
                        drawStopIndicator = {},
                        modifier = Modifier.weight(1f).height(8.dp),
                    )
                    Text(
                        "${seller.sold}",
                        style = MaterialTheme.typography.titleSmall,
                        color = colors.textPrimary,
                        modifier = Modifier.width(40.dp).padding(start = 10.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun KpiTile(label: String, value: String, changePct: Int, modifier: Modifier = Modifier) {
    val colors = FreshSaveTheme.colors
    FsCard(modifier, contentPadding = 14) {
        Text(label, style = MaterialTheme.typography.bodySmall, color = colors.textMuted)
        Text(value, style = MaterialTheme.typography.titleLarge, color = colors.textPrimary, maxLines = 1)
        Text("▲ $changePct%", style = MaterialTheme.typography.labelMedium, color = colors.green)
    }
}

@Preview(widthDp = 380, heightDp = 800)
@Composable
private fun AnalyticsPreview() {
    FreshSaveTheme { AnalyticsScreen(MockData.analytics) }
}
