package com.matteo.chatapp.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
expect fun ProfileBannerImage(
    bannerUrl: String?,
    modifier: Modifier = Modifier
)
