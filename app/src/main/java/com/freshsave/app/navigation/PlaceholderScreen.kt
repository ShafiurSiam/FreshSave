package com.freshsave.app.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.freshsave.app.ui.components.SecondaryButton
import com.freshsave.app.ui.theme.FreshSaveTheme

/** Temporary stand-in used while a screen is still being built. */
@Composable
fun PlaceholderScreen(
    title: String,
    vararg actions: Pair<String, () -> Unit>,
) {
    Column(
        Modifier
            .fillMaxSize()
            .background(FreshSaveTheme.colors.background)
            .systemBarsPadding()
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(title, style = MaterialTheme.typography.headlineSmall, color = FreshSaveTheme.colors.textPrimary)
        Text("Coming soon", style = MaterialTheme.typography.bodyMedium, color = FreshSaveTheme.colors.textMuted)
        actions.forEach { (label, onClick) ->
            SecondaryButton(label, onClick, Modifier.fillMaxWidth())
        }
    }
}
