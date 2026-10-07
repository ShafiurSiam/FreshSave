package com.freshsave.app.feature.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.freshsave.app.ui.appViewModel
import com.freshsave.app.ui.components.CircleIconButton
import com.freshsave.app.ui.theme.FreshSaveTheme

/** Full-screen version of the mini-map. Uses the Home ViewModel's pins. */
@Composable
fun FullMapRoute(onBack: () -> Unit, onDealClick: (String) -> Unit) {
    val viewModel = appViewModel {
        HomeViewModel(it.clock, it.dealRepository, it.shopRepository, it.notificationRepository, it.userRepository)
    }
    val state by viewModel.state.collectAsStateWithLifecycle()
    val colors = FreshSaveTheme.colors
    Box(Modifier.fillMaxSize()) {
        // TODO: Google Maps Compose goes here (see MapPlaceholder).
        MapPlaceholder(
            pins = state.pins,
            onPinClick = { onDealClick(it.dealId) },
            modifier = Modifier.fillMaxSize(),
            pinScale = 1.2f,
        )
        CircleIconButton(
            Icons.AutoMirrored.Filled.ArrowBack, "Back", onBack,
            modifier = Modifier
                .statusBarsPadding()
                .padding(16.dp),
        )
        Text(
            "Deals near ${state.area} · tap a price to open it",
            style = MaterialTheme.typography.titleSmall,
            color = colors.textPrimary,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(20.dp)
                .fillMaxWidth()
                .background(colors.card, RoundedCornerShape(14.dp))
                .padding(16.dp),
        )
    }
}
