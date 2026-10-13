package com.matteo.chatapp.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.runtime.getValue
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.matteo.chatapp.model.Channel
import com.matteo.chatapp.ui.theme.Muted
import com.matteo.chatapp.ui.theme.OnBackground
import com.matteo.chatapp.ui.theme.OnSurface
import com.matteo.chatapp.ui.theme.SurfaceVariant
import com.matteo.chatapp.ui.theme.Primary
import com.matteo.chatapp.ui.theme.PrimarySoft

@Composable
fun ChannelItem(
    channel: Channel,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val background by animateColorAsState(if (isSelected) PrimarySoft else androidx.compose.ui.graphics.Color.Transparent, tween(140))
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(background)
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("#", color = if (isSelected) Primary else Muted, fontSize = 16.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.width(8.dp))
        Text(
            text = channel.name,
            modifier = Modifier.weight(1f),
            color = if (isSelected) OnBackground else OnSurface,
            fontSize = 14.sp,
            fontWeight = if (isSelected || channel.unreadCount > 0) FontWeight.SemiBold else FontWeight.Normal,
            maxLines = 1,
            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
        )
        ActivityBadge(channel.unreadCount, channel.mentionCount)
    }
}
