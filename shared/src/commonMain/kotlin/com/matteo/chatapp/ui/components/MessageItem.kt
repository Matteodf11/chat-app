package com.matteo.chatapp.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.background
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.matteo.chatapp.model.Message
import com.matteo.chatapp.ui.theme.OnBackground
import com.matteo.chatapp.ui.theme.OnSurface
import com.matteo.chatapp.ui.theme.Primary
import com.matteo.chatapp.ui.theme.SurfaceVariant

@Composable
fun MessageItem(message: Message) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(Primary),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = message.senderUsername.take(1).uppercase(),
                color = OnBackground,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = message.senderUsername,
                    color = OnBackground,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = message.createdAt?.take(10) ?: "",
                    color = OnSurface,
                    fontSize = 12.sp
                )
            }
            Text(
                text = message.content,
                color = OnBackground,
                fontSize = 15.sp,
                lineHeight = 20.sp
            )
        }
    }
}