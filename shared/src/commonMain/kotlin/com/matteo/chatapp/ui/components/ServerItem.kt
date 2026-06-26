package com.matteo.chatapp.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.matteo.chatapp.model.Server
import com.matteo.chatapp.ui.theme.OnBackground
import com.matteo.chatapp.ui.theme.Primary
import com.matteo.chatapp.ui.theme.SurfaceVariant

@Composable
fun ServerItem(
    server: Server,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val shape = if (isSelected) RoundedCornerShape(16.dp) else CircleShape
    val bgColor = if (isSelected) Primary else SurfaceVariant

    Box(
        modifier = Modifier
            .size(48.dp)
            .clip(shape)
            .background(bgColor)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = server.name.take(2).uppercase(),
            color = OnBackground,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold
        )
    }
}