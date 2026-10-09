package com.matteo.chatapp.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.matteo.chatapp.ui.theme.Background
import com.matteo.chatapp.ui.theme.Error
import com.matteo.chatapp.ui.theme.Muted
import com.matteo.chatapp.ui.theme.OnBackground
import com.matteo.chatapp.ui.theme.OnPrimary
import com.matteo.chatapp.ui.theme.OnSurface
import com.matteo.chatapp.ui.theme.Outline
import com.matteo.chatapp.ui.theme.Primary
import com.matteo.chatapp.ui.theme.Surface
import com.matteo.chatapp.viewmodel.AuthState
import com.matteo.chatapp.viewmodel.AuthViewModel
import org.jetbrains.compose.resources.painterResource
import chat_app.shared.generated.resources.Res
import chat_app.shared.generated.resources.chimera_logo

@Composable
fun LoginScreen(
    createAuthViewModel: () -> AuthViewModel,
    sessionKey: Int = 0,
    onLoginSuccess: () -> Unit
) {
    val viewModel: AuthViewModel = viewModel(key = "auth-$sessionKey") {
        createAuthViewModel()
    }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var username by remember { mutableStateOf("") }
    var isRegisterMode by remember { mutableStateOf(false) }
    var rememberMe by remember { mutableStateOf(true) }
    val state by viewModel.state.collectAsStateWithLifecycle()

    val submitAuth = {
        if (state !is AuthState.Loading) {
            if (isRegisterMode) viewModel.register(username, email, password)
            else viewModel.login(email, password, rememberMe)
        }
    }

    LaunchedEffect(state) {
        if (state is AuthState.Success) {
            onLoginSuccess()
            viewModel.resetState()
        }
    }

    Box(
        modifier = Modifier.fillMaxSize().background(Background).windowInsetsPadding(WindowInsets.safeDrawing).padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier.widthIn(min = 340.dp, max = 400.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier.size(64.dp).clip(RoundedCornerShape(12.dp)).background(Background),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(Res.drawable.chimera_logo),
                    contentDescription = "Chimera",
                    modifier = Modifier.fillMaxSize()
                )
            }
            Spacer(Modifier.height(22.dp))
            Text(
                text = if (isRegisterMode) "Crear cuenta" else "Iniciar sesion",
                color = OnBackground,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = if (isRegisterMode) "Completa tus datos para continuar" else "Introduce tus datos para entrar",
                color = OnSurface,
                fontSize = 13.sp
            )
            Spacer(Modifier.height(24.dp))

            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                if (isRegisterMode) {
                    AuthField(value = username, onValueChange = { username = it }, label = "Nombre de usuario")
                }
                AuthField(
                    value = email,
                    onValueChange = { email = it },
                    label = "Email",
                    keyboardType = KeyboardType.Email
                )
                AuthField(
                    value = password,
                    onValueChange = { password = it },
                    label = "Contrasena",
                    keyboardType = KeyboardType.Password,
                    isPassword = true,
                    onSubmit = submitAuth
                )

                if (!isRegisterMode) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(
                            checked = rememberMe,
                            onCheckedChange = { rememberMe = it },
                            colors = CheckboxDefaults.colors(checkedColor = Primary)
                        )
                        Text("Recordar este dispositivo", color = OnSurface, fontSize = 13.sp)
                    }
                }

                if (state is AuthState.Error) {
                    Text((state as AuthState.Error).message, color = Error, fontSize = 13.sp)
                }

                Button(
                    onClick = submitAuth,
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Primary),
                    enabled = state !is AuthState.Loading
                ) {
                    if (state is AuthState.Loading) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), color = OnPrimary, strokeWidth = 2.dp)
                    } else {
                        Text(if (isRegisterMode) "Crear cuenta" else "Entrar", color = OnPrimary, fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(Modifier.height(8.dp))
            TextButton(
                onClick = {
                    isRegisterMode = !isRegisterMode
                    viewModel.resetState()
                },
                colors = ButtonDefaults.textButtonColors(contentColor = Primary)
            ) {
                Text(if (isRegisterMode) "Ya tengo una cuenta" else "Crear una cuenta")
            }
        }
    }
}

@Composable
private fun AuthField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    keyboardType: KeyboardType = KeyboardType.Text,
    isPassword: Boolean = false,
    onSubmit: (() -> Unit)? = null
) {
    val focusManager = LocalFocusManager.current
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        modifier = Modifier
            .fillMaxWidth()
            .onPreviewKeyEvent { event ->
                if (onSubmit != null && event.key == Key.Enter && event.type == KeyEventType.KeyDown) {
                    onSubmit()
                    true
                } else false
            },
        shape = RoundedCornerShape(8.dp),
        singleLine = true,
        visualTransformation = if (isPassword) PasswordVisualTransformation() else VisualTransformation.None,
        keyboardOptions = KeyboardOptions(
            keyboardType = keyboardType,
            imeAction = if (onSubmit != null) ImeAction.Done else ImeAction.Next
        ),
        keyboardActions = KeyboardActions(
            onNext = { focusManager.moveFocus(FocusDirection.Next) },
            onDone = { onSubmit?.invoke() }
        ),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = Primary,
            unfocusedBorderColor = Outline,
            focusedLabelColor = Primary,
            unfocusedLabelColor = Muted,
            focusedTextColor = OnBackground,
            unfocusedTextColor = OnBackground,
            cursorColor = Primary,
            focusedContainerColor = Surface,
            unfocusedContainerColor = Surface
        )
    )
}
