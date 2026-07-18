package com.matteo.chatapp.repository

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

object AppVisibility {
    private val _isInForeground = MutableStateFlow(true)
    val isInForeground: StateFlow<Boolean> = _isInForeground

    fun setInForeground(value: Boolean) {
        _isInForeground.value = value
    }
}
