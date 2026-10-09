package com.matteo.chatapp.platform

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect

private const val MESSAGE_CHANNEL = "direct_messages"

actual fun clearMessageNotifications() {
    AndroidPlatform.context.getSystemService(NotificationManager::class.java).cancelAll()
}

@Composable
actual fun PrepareMessageNotifications() {
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }
    LaunchedEffect(Unit) {
        val context = AndroidPlatform.context
        createMessageChannel(context)
        val preferences = context.getSharedPreferences("notification_settings", Context.MODE_PRIVATE)
        if (Build.VERSION.SDK_INT >= 33 &&
            context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED &&
            !preferences.getBoolean("permission_requested", false)) {
            preferences.edit().putBoolean("permission_requested", true).apply()
            launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }
}

actual fun platformMessageNotificationSink() = MessageNotificationSink { message ->
    val context = AndroidPlatform.context
    val manager = context.getSystemService(NotificationManager::class.java)
    if (manager.areNotificationsEnabled()) {
        createMessageChannel(context)
        val intent = context.packageManager.getLaunchIntentForPackage(context.packageName)
        if (intent != null) {
            intent.data = Uri.parse("chimera://dm/${Uri.encode(message.dmId)}")
            intent.putExtra("notification_dm_id", message.dmId)
            val contentIntent = PendingIntent.getActivity(context, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
            val builder = if (Build.VERSION.SDK_INT >= 26) Notification.Builder(context, MESSAGE_CHANNEL)
                else Notification.Builder(context)
            val notification = builder.setSmallIcon(android.R.drawable.stat_notify_chat)
                .setContentTitle(message.sender).setContentText(message.preview)
                .setStyle(Notification.BigTextStyle().bigText(message.preview))
                .setCategory(Notification.CATEGORY_MESSAGE)
                .setAutoCancel(true).setContentIntent(contentIntent).build()
            manager.notify(message.dmId, 1, notification)
        }
    }
}

private fun createMessageChannel(context: Context) {
    if (Build.VERSION.SDK_INT >= 26) {
        context.getSystemService(NotificationManager::class.java).createNotificationChannel(
            NotificationChannel(MESSAGE_CHANNEL, "Mensajes directos", NotificationManager.IMPORTANCE_HIGH)
                .apply { description = "Mensajes nuevos de tus conversaciones" }
        )
    }
}
