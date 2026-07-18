package com.matteo.chatapp.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
expect fun AvatarImage(
    avatarUrl: String?,
    name: String,
    size: Int,
    modifier: Modifier = Modifier
)
