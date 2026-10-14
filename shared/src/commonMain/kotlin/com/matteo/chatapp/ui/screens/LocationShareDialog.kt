package com.matteo.chatapp.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.matteo.chatapp.platform.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.launch
import kotlinx.coroutines.Job

@Composable
internal fun LocationShareDialog(onDismiss: () -> Unit, onSend: (String) -> Unit) {
    var input by remember { mutableStateOf("") }
    var locating by remember { mutableStateOf(false) }
    var manual by remember { mutableStateOf(!supportsCurrentLocation()) }
    var error by remember { mutableStateOf<String?>(null) }
    var locationJob by remember { mutableStateOf<Job?>(null) }
    val scope = rememberCoroutineScope()
    val mapSelection = supportsLocationMap()
    val point = remember(input) { locationFromInput(input) }
    fun selectLocation(automatic: Boolean) {
        locating = true; error = null
        locationJob = scope.launch {
            try {
                val value = if (automatic) currentLocation() else chooseLocationOnMap(point)
                if (value != null) input = "${value.latitude}, ${value.longitude}"
            } catch (_: TimeoutCancellationException) {
                manual = true
                error = if (mapSelection) "La selección ha caducado. Vuelve a abrir el mapa para elegir un punto."
                    else "No se pudo obtener la ubicación a tiempo. Revisa los permisos o elige otro lugar."
            } catch (e: CancellationException) { throw e }
            catch (e: Exception) { manual = true; error = e.message ?: "No se pudo obtener la ubicación" }
            finally { locating = false }
        }
    }
    fun openMap(url: String) {
        runCatching { openExternalUrl(url) }.onFailure { error = "No se pudo abrir el navegador para comprobar el punto." }
    }
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(Modifier.widthIn(max = 560.dp).fillMaxWidth().padding(16.dp).imePadding(), shape = RoundedCornerShape(24.dp)) {
        Column(Modifier.heightIn(max = 600.dp).verticalScroll(rememberScrollState()).padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.LocationOn, null, tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(10.dp))
                Text("Compartir ubicación", Modifier.weight(1f), style = MaterialTheme.typography.titleLarge)
                IconButton(onClick = onDismiss) { Icon(Icons.Rounded.Close, "Cerrar") }
            }
            Text("Elige un punto y compruébalo antes de enviarlo. Se comparte un lugar fijo, no tu ubicación en tiempo real.")
            if (mapSelection) Text("Se abrirá un mapa en tu navegador. Puedes permitir la ubicación o marcar un punto con el ratón.",
                style = MaterialTheme.typography.bodyMedium)
            if (supportsCurrentLocation()) OutlinedButton(enabled = !locating, modifier = Modifier.fillMaxWidth(),
                onClick = { selectLocation(true) }) { Text("Usar mi ubicación") }
            if (mapSelection) OutlinedButton(enabled = !locating, modifier = Modifier.fillMaxWidth(),
                onClick = { selectLocation(false) }) { Text(if (point == null) "Elegir un punto en el mapa" else "Cambiar punto en el mapa") }
            if (locating) Row(verticalAlignment = Alignment.CenterVertically) {
                CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                Spacer(Modifier.width(10.dp)); Text(if (mapSelection) "Selecciona el punto en el navegador" else "Buscando ubicación…", Modifier.weight(1f))
                TextButton(onClick = { locationJob?.cancel() }) { Text("Cancelar") }
            }
            if (!mapSelection && supportsCurrentLocation()) TextButton(enabled = !locating, onClick = { manual = !manual }) {
                Text(if (manual) "Ocultar selección manual" else "Elegir otro lugar")
            }
            if (manual && !mapSelection) {
                OutlinedTextField(input, { input = it.take(2048); error = null }, enabled = !locating,
                    label = { Text("Coordenadas o enlace de Google Maps") },
                    placeholder = { Text("38.345, -0.481") }, maxLines = 3, modifier = Modifier.fillMaxWidth(),
                    isError = input.isNotBlank() && point == null,
                    supportingText = { Text(if (input.isNotBlank() && point == null)
                        "Pega latitud y longitud, o un enlace con coordenadas. Los enlaces cortos y las direcciones de texto no se pueden localizar aquí."
                        else "Puedes usar 38.345, -0.481 o 38,345; -0,481.") })
            }
            point?.let { selected -> Surface(color = MaterialTheme.colorScheme.surfaceVariant, shape = RoundedCornerShape(16.dp)) {
                Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Punto seleccionado", style = MaterialTheme.typography.titleSmall)
                    Text("${selected.latitude}, ${selected.longitude}")
                    Text("La localización del equipo puede ser aproximada. Comprueba el punto en el mapa.", style = MaterialTheme.typography.bodySmall)
                    TextButton(enabled = !locating, onClick = { openMap(selected.mapUrl) }) { Text("Comprobar en el mapa") }
                }
            } }
            error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            if (error != null && supportsCurrentLocation() && !mapSelection) TextButton(onClick = {
                runCatching { openLocationSettings() }.onFailure { error = "Abre los ajustes de ubicación de tu dispositivo y comprueba los permisos." }
            }) { Text("Abrir ajustes de ubicación") }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                TextButton(onClick = onDismiss) { Text("Cancelar") }
                Spacer(Modifier.width(8.dp))
                Button(enabled = point != null && !locating, onClick = {
                    point?.let { onSend(it.message()); onDismiss() }
                }) { Text("Enviar ubicación") }
            }
        }
        }
    }
}
