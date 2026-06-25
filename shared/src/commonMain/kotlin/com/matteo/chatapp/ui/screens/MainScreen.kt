package com.matteo.chatapp.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.matteo.chatapp.repository.SessionManager
import com.matteo.chatapp.ui.theme.Background
import com.matteo.chatapp.ui.theme.OnBackground
import com.matteo.chatapp.ui.theme.Primary

@Composable
fun MainScreen(onLogout: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Background),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "¡Bienvenido, ${SessionManager.getUsername()}!",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = OnBackground
            )
            Text(
                text = "Conectado como ${SessionManager.getEmail()}",
                fontSize = 14.sp,
                color = Primary
            )
        }
    }
}