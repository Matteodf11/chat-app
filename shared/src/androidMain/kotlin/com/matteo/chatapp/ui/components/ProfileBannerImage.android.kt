package com.matteo.chatapp.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import com.matteo.chatapp.ui.theme.PrimarySoft

@Composable
actual fun ProfileBannerImage(
    bannerUrl: String?,
    modifier: Modifier
) {
    var bitmap by remember(bannerUrl) { mutableStateOf<ImageBitmap?>(null) }

    LaunchedEffect(bannerUrl) {
        bitmap = bannerUrl?.takeIf { it.isNotBlank() }?.let { loadAndroidImage(it) }
    }

    val image = bitmap
    if (image != null) {
        Image(
            bitmap = image,
            contentDescription = "Banner de perfil",
            modifier = modifier,
            contentScale = ContentScale.Crop
        )
    } else {
        Box(modifier = modifier.background(PrimarySoft))
    }
}
