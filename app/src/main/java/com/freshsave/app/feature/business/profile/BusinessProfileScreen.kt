package com.freshsave.app.feature.business.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.outlined.HelpOutline
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.Group
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Place
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.ShoppingBag
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.freshsave.app.data.fake.MockData
import com.freshsave.app.data.model.Branch
import com.freshsave.app.data.model.BusinessProfile
import com.freshsave.app.data.model.UserRole
import com.freshsave.app.data.repository.BusinessRepository
import com.freshsave.app.data.repository.SessionRepository
import com.freshsave.app.navigation.LocalSnackbarHostState
import com.freshsave.app.ui.appViewModel
import com.freshsave.app.ui.components.FsSwitch
import com.freshsave.app.ui.components.FsTextField
import com.freshsave.app.ui.components.RowDivider
import com.freshsave.app.ui.components.RowGroup
import com.freshsave.app.ui.components.SectionHeader
import com.freshsave.app.ui.components.SettingsRow
import com.freshsave.app.ui.components.StatusPill
import com.freshsave.app.ui.components.Thumbnail
import com.freshsave.app.ui.theme.FreshSaveTheme
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class BusinessExit { SwitchedToShopper, LoggedOut }

class BusinessProfileViewModel(
    private val businessRepository: BusinessRepository,
    private val sessionRepository: SessionRepository,
) : ViewModel() {
    val profile: StateFlow<BusinessProfile?> = businessRepository.observeProfile()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    private val _exit = MutableStateFlow<BusinessExit?>(null)
    val exit: StateFlow<BusinessExit?> = _exit.asStateFlow()

    fun addBranch(name: String) = viewModelScope.launch { businessRepository.addBranch(name) }
    fun selectBranch(id: String) = viewModelScope.launch { businessRepository.selectBranch(id) }
    fun setAccepting(id: String, accepting: Boolean) = viewModelScope.launch { businessRepository.setBranchAcceptingOrders(id, accepting) }

    fun switchToShopper() = viewModelScope.launch {
        sessionRepository.setRole(UserRole.SHOPPER)
        _exit.value = BusinessExit.SwitchedToShopper
    }

    fun logOut() = viewModelScope.launch {
        sessionRepository.logOut()
        _exit.value = BusinessExit.LoggedOut
    }
}

@Composable
fun BusinessProfileRoute(
    onRatings: () -> Unit,
    onNotifications: () -> Unit,
    onHelp: () -> Unit,
    onSwitchedToShopper: () -> Unit,
    onLoggedOut: () -> Unit,
) {
    val viewModel = appViewModel { BusinessProfileViewModel(it.businessRepository, it.sessionRepository) }
    val profile by viewModel.profile.collectAsStateWithLifecycle()
    val exit by viewModel.exit.collectAsStateWithLifecycle()
    val snackbar = LocalSnackbarHostState.current
    val scope = rememberCoroutineScope()
    var addingBranch by remember { mutableStateOf(false) }

    LaunchedEffect(exit) {
        when (exit) {
            BusinessExit.SwitchedToShopper -> onSwitchedToShopper()
            BusinessExit.LoggedOut -> onLoggedOut()
            null -> Unit
        }
    }

    profile?.let { p ->
        BusinessProfileScreen(
            profile = p,
            onRatings = onRatings,
            onAddBranch = { addingBranch = true },
            onSelectBranch = viewModel::selectBranch,
            onAcceptingChange = viewModel::setAccepting,
            onComingSoon = { what -> scope.launch { snackbar.showSnackbar("$what is coming soon.") } },
            onNotifications = onNotifications,
            onHelp = onHelp,
            onSwitchToShopper = viewModel::switchToShopper,
            onLogOut = viewModel::logOut,
        )
    }

    if (addingBranch) {
        var name by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { addingBranch = false },
            containerColor = FreshSaveTheme.colors.card,
            title = { Text("Add a branch") },
            text = { FsTextField(name, { name = it.take(40) }, placeholder = "e.g. Uttara Sector 7") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.addBranch(name)
                        addingBranch = false
                        scope.launch { snackbar.showSnackbar("$name added — turn it on when it's ready for orders.") }
                    },
                    enabled = name.isNotBlank(),
                ) { Text("Add") }
            },
            dismissButton = { TextButton(onClick = { addingBranch = false }) { Text("Cancel") } },
        )
    }
}

@Composable
fun BusinessProfileScreen(
    profile: BusinessProfile,
    onRatings: () -> Unit,
    onAddBranch: () -> Unit,
    onSelectBranch: (String) -> Unit,
    onAcceptingChange: (String, Boolean) -> Unit,
    onComingSoon: (String) -> Unit,
    onNotifications: () -> Unit,
    onHelp: () -> Unit,
    onSwitchToShopper: () -> Unit,
    onLogOut: () -> Unit,
) {
    val colors = FreshSaveTheme.colors
    Column(
        Modifier
            .fillMaxSize()
            .background(colors.background)
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Thumbnail(size = 64)
            Spacer(Modifier.width(14.dp))
            Column {
                Text(profile.shop.name, style = MaterialTheme.typography.titleLarge, color = colors.textPrimary)
                Row(
                    Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .clickable(onClick = onRatings),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("${profile.shop.kind} · ", style = MaterialTheme.typography.bodyMedium, color = colors.textMuted)
                    Icon(Icons.Filled.Star, null, tint = colors.orange, modifier = Modifier.size(14.dp))
                    Text(" ${profile.shop.rating}", style = MaterialTheme.typography.bodyMedium, color = colors.textMuted)
                }
                if (profile.verified) {
                    Spacer(Modifier.height(4.dp))
                    StatusPill("Verified partner")
                }
            }
        }
        Spacer(Modifier.height(24.dp))
        SectionHeader("Branches", action = "+ Add branch", onAction = onAddBranch)
        Spacer(Modifier.height(10.dp))
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            profile.branches.forEach { branch ->
                BranchRow(
                    branch = branch,
                    selected = branch.id == profile.selectedBranchId,
                    onClick = { onSelectBranch(branch.id) },
                    onAcceptingChange = { onAcceptingChange(branch.id, it) },
                )
            }
        }
        Spacer(Modifier.height(20.dp))
        RowGroup {
            SettingsRow("Ratings & reviews", onRatings, icon = Icons.Outlined.StarOutline)
            RowDivider()
            SettingsRow("Business hours", { onComingSoon("Business hours") }, icon = Icons.Outlined.Schedule)
            RowDivider()
            SettingsRow("Payout account", { onComingSoon("Payout settings") }, icon = Icons.Outlined.AccountBalanceWallet)
            RowDivider()
            SettingsRow("Staff & permissions", { onComingSoon("Staff management") }, icon = Icons.Outlined.Group)
            RowDivider()
            SettingsRow("Notifications", onNotifications, icon = Icons.Outlined.Notifications)
            RowDivider()
            SettingsRow("Help & support", onHelp, icon = Icons.AutoMirrored.Outlined.HelpOutline)
            RowDivider()
            SettingsRow("Switch to shopper account", onSwitchToShopper, icon = Icons.Outlined.ShoppingBag, iconTint = colors.green)
        }
        Spacer(Modifier.height(20.dp))
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
    }
}

@Composable
private fun BranchRow(branch: Branch, selected: Boolean, onClick: () -> Unit, onAcceptingChange: (Boolean) -> Unit) {
    val colors = FreshSaveTheme.colors
    val shape = RoundedCornerShape(16.dp)
    Row(
        Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(colors.card)
            .border(if (selected) 2.dp else 1.dp, if (selected) colors.green else colors.outline, shape)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(40.dp)
                .background(if (selected) colors.greenTint else colors.surfaceMuted, RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Outlined.Place, null, tint = if (selected) colors.green else colors.textMuted, modifier = Modifier.size(20.dp))
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(branch.name, style = MaterialTheme.typography.titleSmall, color = colors.textPrimary)
            val listings = when (branch.liveListings) {
                0 -> "No live listings"
                1 -> "1 live listing"
                else -> "${branch.liveListings} live listings"
            }
            Text(
                if (selected) "Active · $listings" else listings,
                style = MaterialTheme.typography.bodySmall,
                color = if (selected) colors.green else colors.textMuted,
            )
        }
        if (selected) {
            // The switch controls whether this branch accepts new orders.
            FsSwitch(branch.acceptingOrders, onAcceptingChange)
        } else {
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = colors.textMuted)
        }
    }
}

@Preview(widthDp = 380, heightDp = 900)
@Composable
private fun BusinessProfilePreview() {
    FreshSaveTheme {
        BusinessProfileScreen(
            BusinessProfile(MockData.shops.first(), MockData.branches, "banani", verified = true, isPro = true),
            {}, {}, {}, { _, _ -> }, {}, {}, {}, {}, {},
        )
    }
}
