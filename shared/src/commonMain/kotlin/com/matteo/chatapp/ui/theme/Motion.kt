package com.matteo.chatapp.ui.theme

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp

/** Enter only: navigation changes immediately, without keeping an old chat alive. */
@Composable
internal fun contentEntrance(key: Any?): Modifier {
    val progress = remember { Animatable(1f) }
    LaunchedEffect(key) {
        progress.snapTo(0f)
        progress.animateTo(1f, tween(160))
    }
    return Modifier.graphicsLayer {
        alpha = 0.7f + progress.value * 0.3f
        translationY = (1f - progress.value) * 6.dp.toPx()
    }
}
