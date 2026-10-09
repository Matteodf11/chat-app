package com.matteo.chatapp.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.matteo.chatapp.repository.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.CancellationException

sealed class AuthState {
    object Idle : AuthState()
    object Loading : AuthState()
    object Success : AuthState()
    data class Error(val message: String) : AuthState()
}

class AuthViewModel(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _state = MutableStateFlow<AuthState>(AuthState.Idle)
    val state: StateFlow<AuthState> = _state

    fun login(email: String, password: String, rememberMe: Boolean) {
        if (_state.value is AuthState.Loading) return
        if (email.isBlank() || password.isBlank()) {
            _state.value = AuthState.Error("Introduce el email y la contraseña")
            return
        }
        _state.value = AuthState.Loading
        viewModelScope.launch {
            try {
                authRepository.login(email.trim(), password, rememberMe)
                _state.value = AuthState.Success
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _state.value = AuthState.Error(e.message ?: "Email o contraseña incorrectos")
            }
        }
    }

    fun register(username: String, email: String, password: String) {
        if (_state.value is AuthState.Loading) return
        val cleanUsername = username.trim()
        val cleanEmail = email.trim()
        val validationError = when {
            cleanUsername.length !in 3..50 -> "El nombre de usuario debe tener entre 3 y 50 caracteres"
            !EMAIL_PATTERN.matches(cleanEmail) -> "Introduce un email válido"
            password.length !in 10..72 -> "La contraseña debe tener entre 10 y 72 caracteres"
            else -> null
        }
        if (validationError != null) {
            _state.value = AuthState.Error(validationError)
            return
        }
        _state.value = AuthState.Loading
        viewModelScope.launch {
            try {
                authRepository.register(cleanUsername, cleanEmail, password)
                _state.value = AuthState.Success
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _state.value = AuthState.Error(e.message ?: "No se pudo crear la cuenta")
            }
        }
    }

    fun resetState() {
        if (_state.value is AuthState.Loading) return
        _state.value = AuthState.Idle
    }

    private companion object {
        val EMAIL_PATTERN = Regex("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")
    }
}
