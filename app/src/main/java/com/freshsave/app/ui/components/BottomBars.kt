package com.freshsave.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Sell
import androidx.compose.material.icons.outlined.ShoppingBag
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.freshsave.app.R
import com.freshsave.app.ui.theme.FreshSaveTheme

enum class ShopperTab(val label: String) { Home("Home"), Search("Search"), Orders("Orders"), Impact("Impact"), Profile("Profile") }

enum class BusinessTab(val label: String) { Dashboard("Dashboard"), Listings("Listings"), Orders("Orders"), Analytics("Analytics"), Profile("Profile") }

@Composable
private fun ShopperTab.painter(): Painter = when (this) {
    ShopperTab.Home -> rememberVectorPainter(Icons.Outlined.Home)
    ShopperTab.Search -> rememberVectorPainter(Icons.Outlined.Search)
    ShopperTab.Orders -> rememberVectorPainter(Icons.Outlined.ShoppingBag)
    ShopperTab.Impact -> painterResource(R.drawable.ic_leaf)
    ShopperTab.Profile -> rememberVectorPainter(Icons.Outlined.Person)
}

private fun BusinessTab.icon(): ImageVector = when (this) {
    BusinessTab.Dashboard -> Icons.Outlined.GridView
    BusinessTab.Listings -> Icons.Outlined.Sell
    BusinessTab.Orders -> Icons.Outlined.ShoppingBag
    BusinessTab.Analytics -> Icons.Outlined.BarChart
    BusinessTab.Profile -> Icons.Outlined.Person
}

/** Bottom navigation for shoppers: Home · Search · Orders · Impact · Profile. */
@Composable
fun ShopperBottomBar(
    selected: ShopperTab,
    onSelect: (ShopperTab) -> Unit,
    modifier: Modifier = Modifier,
    ordersBadge: Int = 0,
) {
    BarContainer(modifier) {
        ShopperTab.entries.forEach { tab ->
            BarItem(
                label = tab.label,
                painter = tab.painter(),
                selected = tab == selected,
                badge = if (tab == ShopperTab.Orders) ordersBadge else 0,
                onClick = { onSelect(tab) },
                modifier = Modifier.weight(1f),
            )
        }
    }
}

/** Bottom navigation for businesses: Dashboard · Listings · Orders · Analytics · Profile. */
@Composable
fun BusinessBottomBar(
    selected: BusinessTab,
    onSelect: (BusinessTab) -> Unit,
    modifier: Modifier = Modifier,
    ordersBadge: Int = 0,
) {
    BarContainer(modifier) {
        BusinessTab.entries.forEach { tab ->
            BarItem(
                label = tab.label,
                painter = rememberVectorPainter(tab.icon()),
                selected = tab == selected,
                badge = if (tab == BusinessTab.Orders) ordersBadge else 0,
                onClick = { onSelect(tab) },
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun BarContainer(modifier: Modifier, content: @Composable androidx.compose.foundation.layout.RowScope.() -> Unit) {
    Column(modifier.background(FreshSaveTheme.colors.card)) {
        HorizontalDivider(color = FreshSaveTheme.colors.outline)
        Row(
            Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(vertical = 8.dp),
            content = content,
        )
    }
}

@Composable
private fun BarItem(
    label: String,
    painter: Painter,
    selected: Boolean,
    badge: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = FreshSaveTheme.colors
    val tint = if (selected) colors.green else colors.textMuted
    Column(
        modifier = modifier.clickable(
            interactionSource = remember { MutableInteractionSource() },
            indication = ripple(bounded = false, radius = 32.dp),
            onClick = onClick,
        ),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        BadgedBox(badge = {
            if (badge > 0) Badge(containerColor = colors.orange) { Text("$badge") }
        }) {
            Icon(painter, contentDescription = label, tint = tint, modifier = Modifier.size(24.dp))
        }
        Text(
            label,
            style = MaterialTheme.typography.labelMedium,
            color = tint,
            modifier = Modifier.padding(top = 3.dp),
        )
    }
}

@Preview(widthDp = 380)
@Composable
private fun BottomBarsPreview() {
    FreshSaveTheme {
        Column {
            ShopperBottomBar(ShopperTab.Home, {}, ordersBadge = 2)
            BusinessBottomBar(BusinessTab.Dashboard, {})
        }
    }
}
