package com.matteo.chatapp.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.matteo.chatapp.ui.theme.OnBackground
import com.matteo.chatapp.ui.theme.PrimarySoft

@Composable
fun AvatarImage(avatarUrl: String?, name: String, size: Int, modifier: Modifier = Modifier) {
    val pixels = with(LocalDensity.current) { size.dp.roundToPx() }
    val edge = if (pixels <= 256) 256 else if (pixels <= 512) 512 else 1024
    val url = avatarUrl?.takeIf { it.isNotBlank() }
    val key = "$url:$edge"
    var bitmap by remember(key) { mutableStateOf(avatarCache.peek(key)) }
    LaunchedEffect(key) {
        bitmap = url?.let { loadCachedImage(it, edge, key, avatarCache) }
    }
    val frame = modifier.size(size.dp).clip(CircleShape)
    val image = bitmap
    if (image != null) Image(image, "$name avatar", frame, contentScale = ContentScale.Crop,
        filterQuality = androidx.compose.ui.graphics.FilterQuality.High)
    else Box(frame.background(PrimarySoft), contentAlignment = Alignment.Center) {
        Text(name.take(1).uppercase(), color = OnBackground,
            fontSize = (size / 2.6).sp, fontWeight = FontWeight.Bold)
    }
}
