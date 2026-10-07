package com.freshsave.app.feature.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.HelpOutline
import androidx.compose.material.icons.outlined.CreditCard
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Place
import androidx.compose.material.icons.outlined.Storefront
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.freshsave.app.data.fake.MockData
import com.freshsave.app.data.model.UserProfile
import com.freshsave.app.data.model.UserRole
import com.freshsave.app.data.repository.ImpactRepository
import com.freshsave.app.data.repository.SessionRepository
import com.freshsave.app.data.repository.ShopRepository
import com.freshsave.app.data.repository.UserRepository
import com.freshsave.app.navigation.LocalSnackbarHostState
import com.freshsave.app.ui.appViewModel
import com.freshsave.app.ui.components.RowDivider
import com.freshsave.app.ui.components.RowGroup
import com.freshsave.app.ui.components.SettingsRow
import com.freshsave.app.ui.components.StatTile
import com.freshsave.app.ui.components.StatusPill
import com.freshsave.app.ui.theme.FreshSaveTheme
import com.freshsave.app.util.formatTaka
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** One-off results that make the screen navigate away. */
enum class ProfileExit { SwitchedToBusiness, LoggedOut }

data class ProfileUiState(
    val user: UserProfile? = null,
    val moneySaved: Int = 0,
    val rescues: Int = 0,
    val favorites: Int = 0,
    val badge: String = "",
    val exit: ProfileExit? = null,
)

class ProfileViewModel(
    userRepository: UserRepository,
    impactRepository: ImpactRepository,
    shopRepository: ShopRepository,
    private val sessionRepository: SessionRepository,
) : ViewModel() {
    private val exit = MutableStateFlow<ProfileExit?>(null)

    val state: StateFlow<ProfileUiState> = combine(
        userRepository.observeCurrentUser(),
        impactRepository.observeImpact(),
        shopRepository.observeFavorites(),
        exit,
    ) { user, impact, favorites, exitEvent ->
        ProfileUiState(
            user = user,
            moneySaved = impact.moneySaved,
            rescues = impact.rescues,
            favorites = favorites.size,
            badge = "${impact.levelName} L${impact.level}",
            exit = exitEvent,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ProfileUiState())

    fun switchToBusiness() {
        viewModelScope.launch {
            sessionRepository.setRole(UserRole.BUSINESS)
            exit.value = ProfileExit.SwitchedToBusiness
        }
    }

    fun logOut() {
        viewModelScope.launch {
            sessionRepository.logOut()
            exit.value = ProfileExit.LoggedOut
        }
    }

    fun onExitHandled() {
        exit.value = null
    }
}

@Composable
fun ProfileRoute(
    onFavorites: () -> Unit,
    onNotifications: () -> Unit,
    onHelp: () -> Unit,
    onSwitchedToBusiness: () -> Unit,
    onLoggedOut: () -> Unit,
) {
    val viewModel = appViewModel {
        ProfileViewModel(it.userRepository, it.impactRepository, it.shopRepository, it.sessionRepository)
    }
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbar = LocalSnackbarHostState.current
    val scope = rememberCoroutineScope()
    var confirmLogout by remember { mutableStateOf(false) }

    LaunchedEffect(state.exit) {
        when (state.exit) {
            ProfileExit.SwitchedToBusiness -> onSwitchedToBusiness()
            ProfileExit.LoggedOut -> onLoggedOut()
            null -> return@LaunchedEffect
        }
        viewModel.onExitHandled()
    }

    ProfileScreen(
        state = state,
        onFavorites = onFavorites,
        onNotifications = onNotifications,
        onHelp = onHelp,
        onComingSoon = { what -> scope.launch { snackbar.showSnackbar("$what is coming soon.") } },
        onSwitchToBusiness = viewModel::switchToBusiness,
        onLogOut = { confirmLogout = true },
    )

    if (confirmLogout) {
        AlertDialog(
            onDismissRequest = { confirmLogout = false },
            title = { Text("Log out?") },
            text = { Text("You'll need to log in again to reserve deals.") },
            confirmButton = {
                TextButton(onClick = {
                    confirmLogout = false
                    viewModel.logOut()
                }) { Text("Log out", color = FreshSaveTheme.colors.danger) }
            },
            dismissButton = { TextButton(onClick = { confirmLogout = false }) { Text("Cancel") } },
            containerColor = FreshSaveTheme.colors.card,
        )
    }
}

@Composable
fun ProfileScreen(
    state: ProfileUiState,
    onFavorites: () -> Unit,
    onNotifications: () -> Unit,
    onHelp: () -> Unit,
    onComingSoon: (String) -> Unit,
    onSwitchToBusiness: () -> Unit,
    onLogOut: () -> Unit,
) {
    val colors = FreshSaveTheme.colors
    val user = state.user
    Column(
        Modifier
            .fillMaxSize()
            .background(colors.background)
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 20.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(68.dp)
                    .background(colors.green, RoundedCornerShape(18.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Text(user?.initial ?: "", style = MaterialTheme.typography.headlineMedium, color = Color.White)
            }
            Spacer(Modifier.width(16.dp))
            Column {
                Text(user?.name ?: "", style = MaterialTheme.typography.titleLarge, color = colors.textPrimary)
                Text(user?.phone ?: "", style = MaterialTheme.typography.bodyMedium, color = colors.textMuted)
                Spacer(Modifier.height(4.dp))
                if (state.badge.isNotEmpty()) StatusPill("🏅 ${state.badge}")
            }
        }
        Spacer(Modifier.height(20.dp))
        // IntrinsicSize.Min + fillMaxHeight makes all three tiles as tall as the tallest one.
        Row(Modifier.height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            StatTile(formatTaka(state.moneySaved), "saved", Modifier.weight(1f).fillMaxHeight(), centered = true)
            StatTile("${state.rescues}", "rescues", Modifier.weight(1f).fillMaxHeight(), centered = true)
            StatTile("${state.favorites}", "favorites", Modifier.weight(1f).fillMaxHeight(), centered = true, onClick = onFavorites)
        }
        Spacer(Modifier.height(20.dp))
        RowGroup {
            SettingsRow("Payment methods", { onComingSoon("Managing payment methods") }, icon = Icons.Outlined.CreditCard)
            RowDivider()
            SettingsRow("Notifications", onNotifications, icon = Icons.Outlined.Notifications)
            RowDivider()
            SettingsRow("Saved addresses", { onComingSoon("Saved addresses") }, icon = Icons.Outlined.Place)
            RowDivider()
            SettingsRow("Favorites & alerts", onFavorites, icon = Icons.Outlined.FavoriteBorder)
            RowDivider()
            SettingsRow("Help & support", onHelp, icon = Icons.AutoMirrored.Outlined.HelpOutline)
            RowDivider()
            SettingsRow(
                "Switch to business account",
                onSwitchToBusiness,
                icon = Icons.Outlined.Storefront,
                iconTint = colors.orange,
            )
        }
        Spacer(Modifier.height(24.dp))
        Text(
            "Log out",
            style = MaterialTheme.typography.titleMedium,
            color = colors.danger,
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .clip(RoundedCornerShape(10.dp))
                .clickable(onClick = onLogOut)
                .padding(horizontal = 24.dp, vertical = 12.dp),
        )
        Text(
            "FreshSave v0.1.0",
            style = MaterialTheme.typography.bodySmall,
            color = colors.textMuted,
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .padding(top = 4.dp),
        )
    }
}

@Preview(widthDp = 380, heightDp = 800)
@Composable
private fun ProfilePreview() {
    FreshSaveTheme {
        ProfileScreen(
            ProfileUiState(MockData.currentUser, 4280, 23, 5, "Waste Warrior L3"),
            {}, {}, {}, {}, {}, {},
        )
    }
}
