package com.freshsave.app.feature.role

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.ShoppingBag
import androidx.compose.material.icons.outlined.Storefront
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.freshsave.app.data.model.UserRole
import com.freshsave.app.data.repository.SessionRepository
import com.freshsave.app.ui.appViewModel
import com.freshsave.app.ui.components.PrimaryButton
import com.freshsave.app.ui.theme.FreshSaveTheme
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class RoleUiState(val selected: UserRole = UserRole.SHOPPER, val confirmed: UserRole? = null)

class RoleSelectionViewModel(private val sessionRepository: SessionRepository) : ViewModel() {
    private val _state = MutableStateFlow(RoleUiState())
    val state: StateFlow<RoleUiState> = _state.asStateFlow()

    fun select(role: UserRole) = _state.update { it.copy(selected = role) }

    fun confirm() {
        viewModelScope.launch {
            val role = _state.value.selected
            sessionRepository.setRole(role) // Saved, so the next launch opens the right home.
            _state.update { it.copy(confirmed = role) }
        }
    }
}

@Composable
fun RoleSelectionRoute(onRoleChosen: (UserRole) -> Unit) {
    val viewModel = appViewModel { RoleSelectionViewModel(it.sessionRepository) }
    val state by viewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(state.confirmed) { state.confirmed?.let(onRoleChosen) }
    RoleSelectionScreen(state, viewModel::select, viewModel::confirm)
}

@Composable
fun RoleSelectionScreen(
    state: RoleUiState,
    onSelect: (UserRole) -> Unit,
    onContinue: () -> Unit,
) {
    val colors = FreshSaveTheme.colors
    Column(
        Modifier
            .fillMaxSize()
            .background(colors.background)
            .systemBarsPadding()
            .padding(horizontal = 24.dp),
    ) {
        Column(
            Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState()),
        ) {
            Spacer(Modifier.height(48.dp))
            Text("How will you\nuse FreshSave?", style = MaterialTheme.typography.headlineMedium, color = colors.textPrimary)
            Spacer(Modifier.height(10.dp))
            Text("You can switch anytime in settings.", style = MaterialTheme.typography.bodyLarge, color = colors.textMuted)
            Spacer(Modifier.height(28.dp))
            RoleCard(
                icon = Icons.Outlined.ShoppingBag,
                iconTint = colors.green,
                iconBackground = colors.greenTint,
                title = "I'm a shopper",
                body = "Discover surplus deals near me and save up to 70%.",
                selected = state.selected == UserRole.SHOPPER,
                onClick = { onSelect(UserRole.SHOPPER) },
            )
            Spacer(Modifier.height(16.dp))
            RoleCard(
                icon = Icons.Outlined.Storefront,
                iconTint = colors.orange,
                iconBackground = colors.orangeTint,
                title = "I'm a business",
                body = "List surplus stock, recover revenue, cut waste.",
                selected = state.selected == UserRole.BUSINESS,
                onClick = { onSelect(UserRole.BUSINESS) },
            )
        }
        PrimaryButton(
            text = if (state.selected == UserRole.SHOPPER) "Continue as shopper" else "Continue as business",
            onClick = onContinue,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 24.dp),
        )
    }
}

@Composable
private fun RoleCard(
    icon: ImageVector,
    iconTint: Color,
    iconBackground: Color,
    title: String,
    body: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val colors = FreshSaveTheme.colors
    val border by animateColorAsState(if (selected) colors.green else colors.outline, label = "role-border")
    val shape = RoundedCornerShape(20.dp)
    Box(
        Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(colors.card)
            .border(if (selected) 2.dp else 1.dp, border, shape)
            .clickable(onClick = onClick)
            .padding(20.dp),
    ) {
        Column {
            Box(
                Modifier
                    .size(48.dp)
                    .background(iconBackground, RoundedCornerShape(14.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(icon, null, tint = iconTint, modifier = Modifier.size(24.dp))
            }
            Spacer(Modifier.height(16.dp))
            Text(title, style = MaterialTheme.typography.titleLarge, color = colors.textPrimary)
            Spacer(Modifier.height(4.dp))
            Text(body, style = MaterialTheme.typography.bodyMedium, color = colors.textMuted)
        }
        // Filled green check when selected, empty ring otherwise.
        Row(Modifier.align(Alignment.TopEnd)) {
            if (selected) {
                Box(
                    Modifier
                        .size(26.dp)
                        .background(colors.green, CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Filled.Check, null, tint = Color.White, modifier = Modifier.size(16.dp))
                }
            } else {
                Box(
                    Modifier
                        .size(26.dp)
                        .border(2.dp, colors.outline, CircleShape),
                )
            }
        }
    }
}

@Preview(widthDp = 360, heightDp = 760)
@Composable
private fun RolePreview() {
    FreshSaveTheme { RoleSelectionScreen(RoleUiState(), {}, {}) }
}
