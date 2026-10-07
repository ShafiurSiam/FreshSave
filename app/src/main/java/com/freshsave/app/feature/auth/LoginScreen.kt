package com.freshsave.app.feature.auth

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.freshsave.app.navigation.LocalSnackbarHostState
import com.freshsave.app.ui.appViewModel
import com.freshsave.app.ui.components.FsTextField
import com.freshsave.app.ui.components.LeafLogo
import com.freshsave.app.ui.components.PrimaryButton
import com.freshsave.app.ui.components.SecondaryButton
import com.freshsave.app.ui.theme.FreshSaveTheme
import kotlinx.coroutines.launch

@Composable
fun LoginRoute(onAuthenticated: () -> Unit) {
    val viewModel = appViewModel { LoginViewModel(it.sessionRepository) }
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbar = LocalSnackbarHostState.current
    val scope = rememberCoroutineScope()

    // Navigate exactly once when the ViewModel reports success.
    LaunchedEffect(state.authenticated) {
        if (state.authenticated) {
            viewModel.onNavigated()
            onAuthenticated()
        }
    }

    LoginScreen(
        state = state,
        onNameChange = viewModel::onNameChange,
        onIdentifierChange = viewModel::onIdentifierChange,
        onPasswordChange = viewModel::onPasswordChange,
        onToggleShowPassword = viewModel::toggleShowPassword,
        onSubmit = viewModel::submit,
        onSocial = viewModel::socialLogin,
        onToggleMode = viewModel::toggleMode,
        onForgotPassword = { scope.launch { snackbar.showSnackbar("We'll text you a reset link (coming soon).") } },
    )
}

@Composable
fun LoginScreen(
    state: LoginUiState,
    onNameChange: (String) -> Unit,
    onIdentifierChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onToggleShowPassword: () -> Unit,
    onSubmit: () -> Unit,
    onSocial: () -> Unit,
    onToggleMode: () -> Unit,
    onForgotPassword: () -> Unit,
) {
    val colors = FreshSaveTheme.colors
    val isLogin = state.mode == AuthMode.LogIn
    Column(
        Modifier
            .fillMaxSize()
            .background(colors.background)
            .systemBarsPadding()
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 20.dp),
    ) {
        LeafLogo(size = 60.dp)
        Spacer(Modifier.height(28.dp))
        Text(
            if (isLogin) "Welcome back" else "Create account",
            style = MaterialTheme.typography.headlineMedium,
            color = colors.textPrimary,
        )
        Spacer(Modifier.height(6.dp))
        Text(
            if (isLogin) "Log in to rescue tonight's deals." else "Join thousands rescuing food across Dhaka.",
            style = MaterialTheme.typography.bodyLarge,
            color = colors.textMuted,
        )
        Spacer(Modifier.height(24.dp))

        AnimatedVisibility(visible = !isLogin) {
            FsTextField(
                value = state.name,
                onValueChange = onNameChange,
                label = "Full name",
                placeholder = "Ayesha Rahman",
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                modifier = Modifier.padding(bottom = 16.dp),
            )
        }
        FsTextField(
            value = state.identifier,
            onValueChange = onIdentifierChange,
            label = "Phone or email",
            placeholder = "+880 1XXX XXXXXX",
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next),
        )
        Spacer(Modifier.height(16.dp))
        FsTextField(
            value = state.password,
            onValueChange = onPasswordChange,
            label = "Password",
            placeholder = "At least 6 characters",
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
            visualTransformation = if (state.showPassword) VisualTransformation.None else PasswordVisualTransformation(),
            trailing = {
                Text(
                    if (state.showPassword) "Hide" else "Show",
                    style = MaterialTheme.typography.titleSmall,
                    color = colors.green,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable(onClick = onToggleShowPassword)
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                )
            },
        )
        if (isLogin) {
            Text(
                "Forgot password?",
                style = MaterialTheme.typography.titleSmall,
                color = colors.green,
                modifier = Modifier
                    .align(Alignment.End)
                    .padding(top = 8.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .clickable(onClick = onForgotPassword)
                    .padding(4.dp),
            )
        }
        if (state.error != null) {
            Text(
                state.error,
                style = MaterialTheme.typography.bodyMedium,
                color = colors.danger,
                modifier = Modifier.padding(top = 8.dp),
            )
        }
        Spacer(Modifier.height(20.dp))
        PrimaryButton(
            text = if (isLogin) "Log in" else "Sign up",
            onClick = onSubmit,
            loading = state.loading,
            modifier = Modifier.fillMaxWidth(),
        )

        Row(Modifier.padding(vertical = 22.dp), verticalAlignment = Alignment.CenterVertically) {
            HorizontalDivider(Modifier.weight(1f), color = colors.outline)
            Text(
                "or continue with",
                style = MaterialTheme.typography.bodySmall,
                color = colors.textMuted,
                modifier = Modifier.padding(horizontal = 12.dp),
            )
            HorizontalDivider(Modifier.weight(1f), color = colors.outline)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            SecondaryButton("Google", onSocial, Modifier.weight(1f))
            SecondaryButton("Apple", onSocial, Modifier.weight(1f))
        }
        Spacer(Modifier.height(24.dp))
        Text(
            buildAnnotatedString {
                withStyle(SpanStyle(color = colors.textMuted)) {
                    append(if (isLogin) "New here? " else "Already have an account? ")
                }
                withStyle(SpanStyle(color = colors.green, fontWeight = FontWeight.Bold)) {
                    append(if (isLogin) "Create account" else "Log in")
                }
            },
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .clip(RoundedCornerShape(8.dp))
                .clickable(onClick = onToggleMode)
                .padding(8.dp),
        )
    }
}

@Preview(widthDp = 360, heightDp = 800)
@Composable
private fun LoginPreview() {
    FreshSaveTheme {
        LoginScreen(LoginUiState(password = "secret12"), {}, {}, {}, {}, {}, {}, {}, {})
    }
}
