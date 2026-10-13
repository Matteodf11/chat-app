package com.matteo.chatapp.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.matteo.chatapp.model.Server
import com.matteo.chatapp.ui.theme.Primary
import com.matteo.chatapp.ui.theme.SurfaceElevated
import com.matteo.chatapp.ui.theme.SurfaceVariant
import com.matteo.chatapp.ui.theme.Navigation
import com.matteo.chatapp.ui.theme.Outline

@Composable
fun ServerItem(
    server: Server,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val borderColor by animateColorAsState(if (isSelected) Primary else Outline, tween(140))
    Box(modifier = Modifier.width(72.dp).height(48.dp), contentAlignment = Alignment.CenterStart) {
        if (isSelected) {
            Box(
                modifier = Modifier
                    .width(3.dp)
                    .height(30.dp)
                    .clip(RoundedCornerShape(topEnd = 3.dp, bottomEnd = 3.dp))
                    .background(Primary)
            )
        }
        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .size(44.dp)
                .clip(if (isSelected) RoundedCornerShape(12.dp) else CircleShape)
                .background(if (isSelected) Primary else SurfaceElevated)
                .border(2.dp, borderColor, if (isSelected) RoundedCornerShape(12.dp) else CircleShape)
                .clickable { onClick() },
            contentAlignment = Alignment.Center
        ) {
            AvatarImage(avatarUrl = server.iconUrl, name = server.name, size = 44)
        }
        ActivityBadge(server.unreadCount, server.mentionCount, Modifier.align(Alignment.BottomEnd), compact = true)
    }
}
