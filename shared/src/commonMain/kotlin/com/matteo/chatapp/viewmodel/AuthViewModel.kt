package com.matteo.chatapp.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.matteo.chatapp.model.LoginRequest
import com.matteo.chatapp.model.RegisterRequest
import com.matteo.chatapp.network.AuthApi
import com.matteo.chatapp.network.createHttpClient
import com.matteo.chatapp.repository.SessionManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

sealed class AuthState {
    object Idle : AuthState()
    object Loading : AuthState()
    object Success : AuthState()
    data class Error(val message: String) : AuthState()
}

class AuthViewModel : ViewModel() {

    private val api = AuthApi(createHttpClient())

    private val _state = MutableStateFlow<AuthState>(AuthState.Idle)
    val state: StateFlow<AuthState> = _state

    fun login(email: String, password: String) {
        viewModelScope.launch {
            _state.value = AuthState.Loading
            try {
                val response = api.login(LoginRequest(email, password))
                SessionManager.saveSession(response.token, response.username, response.email)
                _state.value = AuthState.Success
            } catch (e: Exception) {
                _state.value = AuthState.Error("Email o contraseña incorrectos")
            }
        }
    }

    fun register(username: String, email: String, password: String) {
        viewModelScope.launch {
            _state.value = AuthState.Loading
            try {
                val response = api.register(RegisterRequest(username, email, password))
                SessionManager.saveSession(response.token, response.username, response.email)
                _state.value = AuthState.Success
            } catch (e: Exception) {
                _state.value = AuthState.Error("Error al registrarse: ${e.message}")
            }
        }
    }

    fun resetState() {
        _state.value = AuthState.Idle
    }
}