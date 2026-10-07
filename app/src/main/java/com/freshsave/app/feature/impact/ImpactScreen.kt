package com.freshsave.app.feature.impact

import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MilitaryTech
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.freshsave.app.data.model.ImpactStats
import com.freshsave.app.data.model.MonthSaving
import com.freshsave.app.data.repository.ImpactRepository
import com.freshsave.app.ui.appViewModel
import com.freshsave.app.ui.components.BarChart
import com.freshsave.app.ui.components.FsCard
import com.freshsave.app.ui.components.StatTile
import com.freshsave.app.ui.theme.FreshSaveTheme
import com.freshsave.app.ui.theme.StatusBarIcons
import com.freshsave.app.util.formatTaka
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

class ImpactViewModel(impactRepository: ImpactRepository) : ViewModel() {
    val state: StateFlow<ImpactStats?> = impactRepository.observeImpact()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
}

@Composable
fun ImpactRoute() {
    val viewModel = appViewModel { ImpactViewModel(it.impactRepository) }
    val stats by viewModel.state.collectAsStateWithLifecycle()
    ImpactScreen(stats)
}

@Composable
fun ImpactScreen(stats: ImpactStats?) {
    StatusBarIcons(lightIcons = true)
    val colors = FreshSaveTheme.colors
    if (stats == null) {
        Box(Modifier.fillMaxSize().background(colors.background), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = colors.green)
        }
        return
    }
    Column(
        Modifier
            .fillMaxSize()
            .background(colors.background)
            .verticalScroll(rememberScrollState()),
    ) {
        // Green header; the stat cards below are pulled up to overlap it.
        Column(
            Modifier
                .fillMaxWidth()
                .background(Brush.verticalGradient(listOf(Color(0xFF2E9A62), colors.green)))
                .statusBarsPadding()
                .padding(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 72.dp),
        ) {
            Text("Your impact so far", style = MaterialTheme.typography.bodyMedium, color = Color.White.copy(alpha = 0.85f))
            Text(
                formatTaka(stats.moneySaved),
                style = MaterialTheme.typography.displayLarge.copy(fontSize = 46.sp, lineHeight = 54.sp),
                color = Color.White,
                maxLines = 1,
                softWrap = false,
            )
            Text(
                "saved across ${stats.rescues} rescues 🎉",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.85f),
            )
        }
        Row(
            Modifier
                .padding(horizontal = 20.dp)
                .offset(y = (-52).dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            StatTile("${stats.mealsRescued}", "meals\nrescued", Modifier.weight(1f))
            StatTile("${stats.foodKg} kg", "food\nsaved", Modifier.weight(1f), valueColor = colors.orange)
            StatTile("${stats.co2Kg} kg", "CO₂\navoided", Modifier.weight(1f))
        }
        Column(
            Modifier
                .padding(horizontal = 20.dp)
                .offset(y = (-36).dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            FsCard {
                Text("Money saved / month", style = MaterialTheme.typography.titleMedium, color = colors.textPrimary)
                Spacer(Modifier.height(16.dp))
                BarChart(
                    values = stats.monthly.map { it.amount },
                    labels = stats.monthly.map { it.label },
                    chartHeight = 110.dp,
                    barSpacing = 14.dp,
                    description = "Money saved per month: " + stats.monthly.joinToString { "${it.label} ${formatTaka(it.amount)}" },
                )
            }
            LevelBadge(stats)
        }
    }
}

@Composable
private fun LevelBadge(stats: ImpactStats) {
    val colors = FreshSaveTheme.colors
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(colors.darkCard)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(52.dp)
                .background(colors.orange, RoundedCornerShape(14.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Filled.MilitaryTech, null, tint = Color.White, modifier = Modifier.size(30.dp))
        }
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text("${stats.levelName} — Level ${stats.level}", style = MaterialTheme.typography.titleMedium, color = Color.White)
            Text(
                if (stats.rescuesToNextLevel > 0) {
                    "${stats.rescuesToNextLevel} more rescue${if (stats.rescuesToNextLevel == 1) "" else "s"} to Level ${stats.level + 1}"
                } else "Top level reached!",
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.7f),
            )
            Spacer(Modifier.height(8.dp))
            // Rough progress within the current level band.
            val band = (stats.rescuesToNextLevel + 3).coerceAtLeast(1)
            LinearProgressIndicator(
                progress = { 1f - stats.rescuesToNextLevel.toFloat() / band },
                color = colors.orange,
                trackColor = Color.White.copy(alpha = 0.15f),
                strokeCap = StrokeCap.Round,
                gapSize = 0.dp,
                drawStopIndicator = {},
                modifier = Modifier.fillMaxWidth().height(6.dp),
            )
        }
    }
}

@Preview(widthDp = 380, heightDp = 800)
@Composable
private fun ImpactPreview() {
    FreshSaveTheme {
        ImpactScreen(
            ImpactStats(
                4280, 23, 23, 18, 46,
                listOf(MonthSaving("Jun", 420), MonthSaving("Jul", 760), MonthSaving("Aug", 640), MonthSaving("Sep", 1050), MonthSaving("Oct", 1410)),
                3, "Waste Warrior", 2,
            ),
        )
    }
}
