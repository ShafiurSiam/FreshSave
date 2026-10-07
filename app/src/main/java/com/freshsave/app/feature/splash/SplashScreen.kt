package com.freshsave.app.feature.splash

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.freshsave.app.data.model.UserRole
import com.freshsave.app.data.repository.SessionRepository
import com.freshsave.app.ui.appViewModel
import com.freshsave.app.ui.components.LeafLogo
import com.freshsave.app.ui.theme.FreshSaveTheme
import com.freshsave.app.ui.theme.StatusBarIcons
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/** Where the splash screen sends you, based on what DataStore remembers. */
enum class SplashDestination { Onboarding, Login, RoleSelection, ShopperHome, BusinessDashboard }

class SplashViewModel(private val sessionRepository: SessionRepository) : ViewModel() {
    private val _destination = MutableStateFlow<SplashDestination?>(null)
    val destination: StateFlow<SplashDestination?> = _destination.asStateFlow()

    init {
        viewModelScope.launch {
            // Read the saved session, then keep the logo up for a moment so it
            // doesn't just flash past.
            val session = sessionRepository.session.first()
            delay(1_600)
            _destination.value = when {
                session.loggedIn && session.role == UserRole.SHOPPER -> SplashDestination.ShopperHome
                session.loggedIn && session.role == UserRole.BUSINESS -> SplashDestination.BusinessDashboard
                session.loggedIn -> SplashDestination.RoleSelection
                session.onboardingSeen -> SplashDestination.Login
                else -> SplashDestination.Onboarding
            }
        }
    }
}

@Composable
fun SplashRoute(onFinished: (SplashDestination) -> Unit) {
    val viewModel = appViewModel { SplashViewModel(it.sessionRepository) }
    val destination by viewModel.destination.collectAsStateWithLifecycle()
    LaunchedEffect(destination) { destination?.let(onFinished) }
    SplashScreen()
}

@Composable
fun SplashScreen() {
    StatusBarIcons(lightIcons = true)
    val colors = FreshSaveTheme.colors
    // Logo fades and grows in once when the screen appears.
    val intro = remember { Animatable(0f) }
    LaunchedEffect(Unit) { intro.animateTo(1f, tween(700)) }

    Box(
        Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(listOf(Color(0xFF2E9A62), colors.green, Color(0xFF13643C))),
            ),
    ) {
        Column(
            Modifier
                .align(Alignment.Center)
                .alpha(intro.value)
                .scale(0.9f + 0.1f * intro.value),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            LeafLogo(size = 104.dp, background = Color.White.copy(alpha = 0.16f))
            Spacer(Modifier.height(28.dp))
            Text(
                "FreshSave",
                style = MaterialTheme.typography.displayMedium.copy(fontSize = 44.sp),
                color = Color.White,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                "Rescue good food. Save good money.",
                style = MaterialTheme.typography.bodyLarge,
                color = Color.White.copy(alpha = 0.85f),
            )
        }
        ProgressDots(
            Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 48.dp),
        )
    }
}

/** Three dots where the orange "active" dot hops along while we load. */
@Composable
private fun ProgressDots(modifier: Modifier = Modifier) {
    var active by remember { mutableIntStateOf(0) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(450)
            active = (active + 1) % 3
        }
    }
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        repeat(3) { index ->
            val color by animateColorAsState(
                if (index == active) FreshSaveTheme.colors.orange else Color.White.copy(alpha = 0.4f),
                label = "dot",
            )
            Box(
                Modifier
                    .size(8.dp)
                    .background(color, CircleShape),
            )
        }
    }
}

@Preview(widthDp = 360, heightDp = 760)
@Composable
private fun SplashPreview() {
    FreshSaveTheme { SplashScreen() }
}
