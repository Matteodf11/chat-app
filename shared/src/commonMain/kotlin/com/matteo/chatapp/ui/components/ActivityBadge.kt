package com.matteo.chatapp.ui.components

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.matteo.chatapp.ui.theme.*

@Composable
fun ActivityBadge(unread: Long, mentions: Long, modifier: Modifier = Modifier, compact: Boolean = false) {
    if (unread <= 0 && mentions <= 0) return
    val background = Primary
    // Choose the higher-contrast text for the user's accent, including bright ones.
    val foreground = if (background.luminance() > 0.179f) Color.Black else Color.White
    fun count(value: Long) = if (value > 99) "99+" else value.coerceAtLeast(0).toString()
    val label = if (mentions > 0) "${count(unread)}${if (compact) " " else " · "}@${count(mentions)}" else count(unread)
    Surface(modifier.semantics { contentDescription = "$unread mensajes sin leer, $mentions menciones para ti" },
        color = background, shape = RoundedCornerShape(9.dp)) {
        Text(label,
            color = foreground, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center,
            fontSize = if (compact) 11.sp else 12.sp, lineHeight = 16.sp, maxLines = 1,
            modifier = Modifier.defaultMinSize(minWidth = 24.dp).padding(horizontal = if (compact) 4.dp else 7.dp, vertical = 3.dp))
    }
}
