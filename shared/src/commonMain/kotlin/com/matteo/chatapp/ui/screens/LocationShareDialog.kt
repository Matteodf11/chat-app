package com.matteo.chatapp.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.matteo.chatapp.platform.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.launch

@Composable
internal fun LocationShareDialog(onDismiss: () -> Unit, onSend: (String) -> Unit) {
    var latitude by remember { mutableStateOf("") }
    var longitude by remember { mutableStateOf("") }
    var locating by remember { mutableStateOf(false) }
    var manual by remember { mutableStateOf(!supportsCurrentLocation()) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    val point = runCatching { SharedLocation(latitude.replace(',', '.').toDouble(), longitude.replace(',', '.').toDouble()) }.getOrNull()
    AlertDialog(onDismissRequest = onDismiss, title = { Text("Compartir ubicación") }, text = {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Revisa el lugar antes de enviarlo. Se comparte un punto fijo en el mapa.")
            if (supportsCurrentLocation()) OutlinedButton(enabled = !locating, onClick = {
                locating = true; error = null
                scope.launch {
                    try { val value = currentLocation(); latitude = value.latitude.toString(); longitude = value.longitude.toString() }
                    catch (_: TimeoutCancellationException) { error = "No se pudo obtener la ubicación a tiempo. Puedes introducir las coordenadas." }
                    catch (e: CancellationException) { throw e }
                    catch (e: Exception) { error = e.message ?: "No se pudo obtener la ubicación" }
                    finally { locating = false }
                }
            }) { Text(if (locating) "Localizando…" else "Usar mi ubicación") }
            if (supportsCurrentLocation()) TextButton(onClick = { manual = !manual }) { Text(if (manual) "Ocultar coordenadas" else "Introducir otro lugar por coordenadas") }
            if (manual) {
                OutlinedTextField(latitude, { latitude = it }, label = { Text("Latitud (−90 a 90)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(longitude, { longitude = it }, label = { Text("Longitud (−180 a 180)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            }
            point?.let { TextButton(onClick = { openExternalUrl(it.mapUrl) }) { Text("Comprobar en el mapa") } }
            error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        }
    }, confirmButton = { TextButton(enabled = point != null && !locating, onClick = { point?.let { onSend(it.message()) }; onDismiss() }) { Text("Enviar ubicación") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } })
}
