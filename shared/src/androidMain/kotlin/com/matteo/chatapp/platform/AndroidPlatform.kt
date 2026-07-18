package com.matteo.chatapp.platform

import android.content.Context
import androidx.activity.ComponentActivity
import com.matteo.chatapp.repository.AppVisibility

internal object AndroidPlatform {
    lateinit var context: Context
        private set

    fun initialize(activity: ComponentActivity) {
        context = activity.applicationContext
        AndroidProfileImagePicker.initialize(activity)
        AndroidAttachmentFilePicker.initialize(activity)
        AndroidVoiceRecorder.initialize(activity)
    }

    fun setInForeground(value: Boolean) {
        AppVisibility.setInForeground(value)
    }
}

fun initializeAndroidPlatform(activity: ComponentActivity) {
    AndroidPlatform.initialize(activity)
}

fun setAndroidAppInForeground(value: Boolean) {
    AndroidPlatform.setInForeground(value)
}
