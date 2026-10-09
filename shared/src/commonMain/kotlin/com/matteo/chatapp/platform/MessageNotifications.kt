package com.matteo.chatapp.platform

import androidx.compose.runtime.Composable
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

data class DirectMessageNotification(val messageId: String, val dmId: String, val sender: String, val preview: String)

fun interface MessageNotificationSink {
    fun show(notification: DirectMessageNotification)
}

expect fun platformMessageNotificationSink(): MessageNotificationSink
expect fun clearMessageNotifications()

@Composable
expect fun PrepareMessageNotifications()

object NotificationNavigation {
    private val _pendingDm = MutableStateFlow<String?>(null)
    val pendingDm = _pendingDm.asStateFlow()
    fun openDm(dmId: String) { _pendingDm.value = dmId }
    fun consume(dmId: String) { _pendingDm.compareAndSet(dmId, null) }
    fun clear() { _pendingDm.value = null }
}
