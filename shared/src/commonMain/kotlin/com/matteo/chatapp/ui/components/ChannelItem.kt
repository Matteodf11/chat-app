package com.matteo.chatapp.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.matteo.chatapp.model.Channel
import com.matteo.chatapp.ui.theme.OnBackground
import com.matteo.chatapp.ui.theme.OnSurface
import com.matteo.chatapp.ui.theme.SurfaceVariant

@Composable
fun ChannelItem(
    channel: Channel,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val bgColor = if (isSelected) SurfaceVariant else androidx.compose.ui.graphics.Color.Transparent
    val textColor = if (isSelected) OnBackground else OnSurface

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(4.dp))
            .background(bgColor)
            .clickable { onClick() }
            .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "# ${channel.name}",
            color = textColor,
            fontSize = 15.sp,
            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
        )
    }
}