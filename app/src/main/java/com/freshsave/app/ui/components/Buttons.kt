package com.freshsave.app.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.freshsave.app.ui.theme.FreshSaveTheme

private val ButtonShape = RoundedCornerShape(14.dp)

/**
 * The big rounded green button used for every main action ("Log in", "Reserve now").
 * Set [loading] to swap the label for a spinner while a fake network call runs.
 */
@Composable
fun PrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    loading: Boolean = false,
    containerColor: Color = FreshSaveTheme.colors.green,
    contentColor: Color = Color.White,
    leadingIcon: ImageVector? = null,
) {
    Button(
        onClick = onClick,
        enabled = enabled && !loading,
        shape = ButtonShape,
        colors = ButtonDefaults.buttonColors(
            containerColor = containerColor,
            contentColor = contentColor,
            disabledContainerColor = containerColor.copy(alpha = if (loading) 1f else 0.4f),
            disabledContentColor = contentColor.copy(alpha = 0.9f),
        ),
        contentPadding = PaddingValues(horizontal = 20.dp),
        modifier = modifier.height(54.dp),
    ) {
        AnimatedContent(targetState = loading, label = "button-loading") { isLoading ->
            if (isLoading) {
                CircularProgressIndicator(color = contentColor, strokeWidth = 2.5.dp, modifier = Modifier.size(22.dp))
            } else {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (leadingIcon != null) Icon(leadingIcon, contentDescription = null, modifier = Modifier.size(20.dp))
                    Text(text, style = MaterialTheme.typography.labelLarge)
                }
            }
        }
    }
}

/** White outlined button that sits beside a [PrimaryButton] ("Reset", "Directions"). */
@Composable
fun SecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    leadingIcon: ImageVector? = null,
) {
    OutlinedButton(
        onClick = onClick,
        shape = ButtonShape,
        border = BorderStroke(1.dp, FreshSaveTheme.colors.outline),
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = FreshSaveTheme.colors.card,
            contentColor = FreshSaveTheme.colors.textPrimary,
        ),
        contentPadding = PaddingValues(horizontal = 16.dp),
        modifier = modifier.height(54.dp),
    ) {
        if (leadingIcon != null) {
            Icon(leadingIcon, contentDescription = null, modifier = Modifier.size(18.dp))
            androidx.compose.foundation.layout.Spacer(Modifier.size(6.dp))
        }
        Text(text, style = MaterialTheme.typography.labelLarge)
    }
}

/** Flat, tinted button used inside cards ("Edit", "Expire now", "Extend window"). */
@Composable
fun TonalButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    container: Color = FreshSaveTheme.colors.surfaceMuted,
    content: Color = FreshSaveTheme.colors.textPrimary,
    leadingIcon: ImageVector? = null,
) {
    Row(
        modifier = modifier
            .height(40.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(container)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        if (leadingIcon != null) {
            Icon(leadingIcon, null, tint = content, modifier = Modifier.size(16.dp).padding(end = 4.dp))
        }
        Text(text, color = content, style = MaterialTheme.typography.labelLarge)
    }
}

/**
 * Pill-shaped choice chip from the search filters. Selected = solid green.
 * (Named FilterChip to match the spec; it is our own, not Material's.)
 */
@Composable
fun FilterChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = FreshSaveTheme.colors
    Text(
        text = label,
        style = MaterialTheme.typography.labelLarge.copy(fontSize = MaterialTheme.typography.bodyMedium.fontSize),
        color = if (selected) Color.White else colors.textPrimary,
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(if (selected) colors.green else colors.card)
            .border(1.dp, if (selected) colors.green else colors.outline, RoundedCornerShape(50))
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 9.dp),
    )
}

/** Beige track with a white "thumb" behind the selected option ("Active | Past"). */
@Composable
fun SegmentedTabs(
    options: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = FreshSaveTheme.colors
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(colors.surfaceMuted)
            .padding(4.dp),
    ) {
        options.forEachIndexed { index, option ->
            val selected = index == selectedIndex
            Text(
                text = option,
                style = MaterialTheme.typography.titleSmall,
                color = if (selected) colors.textPrimary else colors.textMuted,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (selected) colors.card else Color.Transparent)
                    .clickable { onSelect(index) }
                    .padding(vertical = 10.dp),
            )
        }
    }
}

@Preview
@Composable
private fun ButtonsPreview() {
    FreshSaveTheme {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            PrimaryButton("Reserve now", {}, Modifier.fillMaxWidth())
            PrimaryButton("Paying", {}, Modifier.fillMaxWidth(), loading = true)
            PrimaryButton("Post surplus listing", {}, Modifier.fillMaxWidth(), containerColor = FreshSaveTheme.colors.orange)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip("Under ৳100", true, {})
                FilterChip("৳100–300", false, {})
            }
            SegmentedTabs(listOf("Active", "Past"), 0, {})
        }
    }
}
