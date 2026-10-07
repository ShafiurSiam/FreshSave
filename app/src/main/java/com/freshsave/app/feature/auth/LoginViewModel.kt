package com.freshsave.app.feature.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.freshsave.app.data.repository.SessionRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class AuthMode { LogIn, SignUp }

/** Everything the login screen draws. The screen never changes this directly. */
data class LoginUiState(
    val mode: AuthMode = AuthMode.LogIn,
    val name: String = "",
    val identifier: String = "+880 1712 345678",
    val password: String = "",
    val showPassword: Boolean = false,
    val loading: Boolean = false,
    val error: String? = null,
    /** Flips to true once authenticated; the screen then navigates on. */
    val authenticated: Boolean = false,
)

class LoginViewModel(private val sessionRepository: SessionRepository) : ViewModel() {
    private val _state = MutableStateFlow(LoginUiState())
    val state: StateFlow<LoginUiState> = _state.asStateFlow()

    fun onNameChange(value: String) = _state.update { it.copy(name = value, error = null) }
    fun onIdentifierChange(value: String) = _state.update { it.copy(identifier = value, error = null) }
    fun onPasswordChange(value: String) = _state.update { it.copy(password = value, error = null) }
    fun toggleShowPassword() = _state.update { it.copy(showPassword = !it.showPassword) }

    fun toggleMode() = _state.update {
        it.copy(mode = if (it.mode == AuthMode.LogIn) AuthMode.SignUp else AuthMode.LogIn, error = null)
    }

    fun submit() {
        val s = _state.value
        val error = when {
            s.mode == AuthMode.SignUp && s.name.isBlank() -> "Please enter your name."
            s.identifier.isBlank() -> "Enter your phone number or email."
            s.password.length < 6 -> "Password must be at least 6 characters."
            else -> null
        }
        if (error != null) {
            _state.update { it.copy(error = error) }
            return
        }
        authenticate()
    }

    /** Google / Apple are stubs for now: they just sign you in. */
    fun socialLogin() = authenticate()

    private fun authenticate() {
        viewModelScope.launch {
            _state.update { it.copy(loading = true, error = null) }
            delay(700) // Fake network round-trip.
            // TODO: replace with Firebase Auth / REST login once a backend exists.
            sessionRepository.logIn()
            _state.update { it.copy(loading = false, authenticated = true) }
        }
    }

    fun onNavigated() = _state.update { it.copy(authenticated = false) }
}
