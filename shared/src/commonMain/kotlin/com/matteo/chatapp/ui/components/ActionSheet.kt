package com.matteo.chatapp.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.matteo.chatapp.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ActionSheet(title: String, onDismiss: () -> Unit, avatarName: String? = null,
    avatarUrl: String? = null, content: @Composable ColumnScope.() -> Unit) {
    ModalBottomSheet(onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        sheetMaxWidth = 560.dp, containerColor = Surface, contentColor = OnBackground) {
        Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(start = 20.dp, end = 20.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(Modifier.fillMaxWidth().padding(bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                if (avatarName != null) AvatarImage(avatarUrl, avatarName, 48)
                Text(title, style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f),
                    maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
            content()
        }
    }
}

@Composable
internal fun ActionSheetItem(label: String, icon: ImageVector, onClick: () -> Unit) {
    Surface(onClick = onClick, modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium, color = SurfaceVariant) {
        ListItem(headlineContent = { Text(label) },
            leadingContent = { Icon(icon, null, tint = Primary) },
            colors = ListItemDefaults.colors(containerColor = SurfaceVariant, headlineColor = OnBackground))
    }
}
