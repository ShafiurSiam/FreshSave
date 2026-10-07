package com.freshsave.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.Inbox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.freshsave.app.R
import com.freshsave.app.ui.theme.FreshSaveTheme

/** Green rounded square with the white leaf — the FreshSave app mark. */
@Composable
fun LeafLogo(
    modifier: Modifier = Modifier,
    size: Dp = 56.dp,
    background: Color = FreshSaveTheme.colors.green,
) {
    Box(
        modifier
            .size(size)
            .clip(RoundedCornerShape(size * 0.28f))
            .background(background),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painterResource(R.drawable.ic_leaf),
            contentDescription = "FreshSave",
            tint = Color.White,
            modifier = Modifier.size(size * 0.48f),
        )
    }
}

enum class NavIcon { Back, Close }

/** Simple top row: back/close button and a bold title, with optional actions on the right. */
@Composable
fun FsTopBar(
    title: String,
    modifier: Modifier = Modifier,
    navIcon: NavIcon? = NavIcon.Back,
    onNavClick: () -> Unit = {},
    actions: @Composable () -> Unit = {},
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp)
            .padding(horizontal = if (navIcon == null) 20.dp else 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (navIcon != null) {
            IconButton(onClick = onNavClick) {
                Icon(
                    if (navIcon == NavIcon.Back) Icons.AutoMirrored.Filled.ArrowBack else Icons.Filled.Close,
                    contentDescription = if (navIcon == NavIcon.Back) "Back" else "Close",
                    tint = FreshSaveTheme.colors.textPrimary,
                )
            }
            Spacer(Modifier.width(4.dp))
        }
        Text(
            title,
            style = MaterialTheme.typography.titleLarge,
            color = FreshSaveTheme.colors.textPrimary,
            modifier = Modifier.weight(1f),
        )
        actions()
    }
}

/** Large page title used on tab screens ("My orders", "Favorites"). */
@Composable
fun ScreenTitle(title: String, modifier: Modifier = Modifier, subtitle: String? = null, trailing: @Composable () -> Unit = {}) {
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.headlineSmall, color = FreshSaveTheme.colors.textPrimary)
            if (subtitle != null) {
                Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = FreshSaveTheme.colors.textMuted)
            }
        }
        trailing()
    }
}

/** Section label with an optional green action on the right ("Ending soon … See all"). */
@Composable
fun SectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    action: String? = null,
    onAction: () -> Unit = {},
) {
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(
            title,
            style = MaterialTheme.typography.titleMedium,
            color = FreshSaveTheme.colors.textPrimary,
            modifier = Modifier.weight(1f),
        )
        if (action != null) {
            Text(
                action,
                style = MaterialTheme.typography.titleSmall,
                color = FreshSaveTheme.colors.green,
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .clickable(onClick = onAction)
                    .padding(4.dp),
            )
        }
    }
}

/** Round white button floating on a photo (back, favourite). */
@Composable
fun CircleIconButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tint: Color = FreshSaveTheme.colors.textPrimary,
    background: Color = FreshSaveTheme.colors.card,
) {
    Box(
        modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(background)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription, tint = tint, modifier = Modifier.size(20.dp))
    }
}

/** Coloured circle with an initial ("A" for Ayesha). */
@Composable
fun Avatar(initial: String, modifier: Modifier = Modifier, color: Color = FreshSaveTheme.colors.green, size: Dp = 36.dp) {
    Box(
        modifier
            .size(size)
            .clip(CircleShape)
            .background(color),
        contentAlignment = Alignment.Center,
    ) {
        Text(initial, color = Color.White, style = MaterialTheme.typography.titleSmall)
    }
}

/** Picks a stable avatar colour from a name, like the mixed avatars in the pickup queue. */
@Composable
fun avatarColorFor(name: String): Color {
    val palette = listOf(FreshSaveTheme.colors.green, FreshSaveTheme.colors.orange, Color(0xFF3B82F6), Color(0xFF8B5CF6))
    return palette[(name.hashCode() and 0x7fffffff) % palette.size]
}

/** Row of 1–5 stars. Pass [onRate] to make it tappable. */
@Composable
fun StarRow(
    rating: Int,
    modifier: Modifier = Modifier,
    starSize: Dp = 14.dp,
    spacing: Dp = 1.dp,
    onRate: ((Int) -> Unit)? = null,
) {
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(spacing)) {
        for (i in 1..5) {
            Icon(
                Icons.Filled.Star,
                contentDescription = if (onRate != null) "$i star${if (i > 1) "s" else ""}" else null,
                tint = if (i <= rating) FreshSaveTheme.colors.orange else FreshSaveTheme.colors.outline,
                modifier = Modifier
                    .size(starSize)
                    .then(if (onRate != null) Modifier.clip(CircleShape).clickable { onRate(i) } else Modifier),
            )
        }
    }
}

/** Labelled rounded text field matching the login form. */
@Composable
fun FsTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    label: String? = null,
    placeholder: String? = null,
    singleLine: Boolean = true,
    minLines: Int = 1,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    leadingIcon: (@Composable () -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null,
    isError: Boolean = false,
) {
    val colors = FreshSaveTheme.colors
    Column(modifier) {
        if (label != null) {
            Text(label, style = MaterialTheme.typography.labelMedium, color = colors.textPrimary)
            Spacer(Modifier.height(6.dp))
        }
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth(),
            placeholder = placeholder?.let { { Text(it, color = colors.textMuted) } },
            singleLine = singleLine,
            minLines = minLines,
            keyboardOptions = keyboardOptions,
            visualTransformation = visualTransformation,
            leadingIcon = leadingIcon,
            trailingIcon = trailing,
            isError = isError,
            textStyle = MaterialTheme.typography.bodyLarge.copy(color = colors.textPrimary),
            shape = RoundedCornerShape(14.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = colors.card,
                unfocusedContainerColor = colors.card,
                focusedBorderColor = colors.green,
                unfocusedBorderColor = colors.outline,
                cursorColor = colors.green,
            ),
        )
    }
}

/** Friendly message shown when a list has nothing in it. */
@Composable
fun EmptyState(
    title: String,
    message: String,
    modifier: Modifier = Modifier,
    icon: ImageVector = Icons.Outlined.Inbox,
    actionLabel: String? = null,
    onAction: () -> Unit = {},
) {
    Column(
        modifier
            .fillMaxWidth()
            .padding(horizontal = 32.dp, vertical = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            Modifier
                .size(72.dp)
                .clip(CircleShape)
                .background(FreshSaveTheme.colors.greenTint),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, null, tint = FreshSaveTheme.colors.green, modifier = Modifier.size(32.dp))
        }
        Spacer(Modifier.height(16.dp))
        Text(title, style = MaterialTheme.typography.titleMedium, color = FreshSaveTheme.colors.textPrimary, textAlign = TextAlign.Center)
        Spacer(Modifier.height(4.dp))
        Text(message, style = MaterialTheme.typography.bodyMedium, color = FreshSaveTheme.colors.textMuted, textAlign = TextAlign.Center)
        if (actionLabel != null) {
            Spacer(Modifier.height(16.dp))
            Text(
                actionLabel,
                style = MaterialTheme.typography.labelLarge,
                color = FreshSaveTheme.colors.green,
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .border(1.dp, FreshSaveTheme.colors.green, RoundedCornerShape(50))
                    .clickable(onClick = onAction)
                    .padding(horizontal = 18.dp, vertical = 8.dp),
            )
        }
    }
}

@Preview(widthDp = 360)
@Composable
private fun CommonPreview() {
    FreshSaveTheme {
        Column(Modifier.background(FreshSaveTheme.colors.background).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            LeafLogo()
            FsTopBar("Checkout")
            SectionHeader("Ending soon", action = "See all")
            StarRow(4, starSize = 24.dp)
            FsTextField("+880 1712 345678", {}, label = "Phone or email")
            EmptyState("No active orders", "Reserve a deal and it will show up here.", actionLabel = "Browse deals")
        }
    }
}
