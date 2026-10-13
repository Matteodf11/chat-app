package com.matteo.chatapp.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.matteo.chatapp.platform.*
import com.matteo.chatapp.ui.components.thumbnailCache
import com.matteo.chatapp.ui.components.avatarCache
import com.matteo.chatapp.ui.theme.*
import kotlinx.coroutines.launch

@Composable
internal fun StorageSettings() {
    val thumbnailBytes by thumbnailCache.bytes.collectAsState()
    val avatarBytes by avatarCache.bytes.collectAsState()
    val memoryBytes = thumbnailBytes + avatarBytes
    var disk by remember { mutableStateOf<TemporaryMediaStats?>(null) }
    var working by remember { mutableStateOf(false) }
    var notice by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    LaunchedEffect(Unit) {
        try { disk = temporaryMediaStats() }
        catch (e: kotlinx.coroutines.CancellationException) { throw e }
        catch (_: Exception) { notice = "No se pudo consultar el almacenamiento" }
    }
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("Almacenamiento", style = MaterialTheme.typography.titleMedium, color = OnBackground)
        StorageSizeRow("Imágenes en memoria", memoryBytes)
        disk?.let {
            StorageSizeRow("Archivos temporales", it.bytes)
            StorageSizeRow("Temporales que se pueden limpiar", it.clearableBytes)
        }
        Text("Puedes liberar las miniaturas y los temporales de más de un día. Tus conversaciones, borradores, descargas y envíos pendientes se conservan.",
            style = MaterialTheme.typography.bodyMedium, color = Muted)
        OutlinedButton(enabled = !working && (memoryBytes > 0 || (disk?.clearableBytes ?: 0) > 0),
            onClick = {
                working = true; notice = null
                scope.launch {
                    try {
                        thumbnailCache.clear()
                        avatarCache.clear()
                        clearOldTemporaryMedia()
                        disk = temporaryMediaStats()
                        notice = "Caché liberada"
                    } catch (e: kotlinx.coroutines.CancellationException) { throw e }
                    catch (_: Exception) { notice = "No se pudieron limpiar todos los temporales" }
                    finally { working = false }
                }
            }) { Text(if (working) "Limpiando…" else "Limpiar caché") }
        notice?.let { Text(it, color = OnSurface, style = MaterialTheme.typography.bodyMedium) }
    }
}

@Composable
private fun StorageSizeRow(label: String, bytes: Long) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(label, Modifier.weight(1f), color = OnSurface, style = MaterialTheme.typography.bodyMedium)
        Text(if (bytes < 1024 * 1024) "${(bytes + 1023) / 1024} KB" else "${(bytes + 524288) / 1048576} MB",
            color = OnBackground, style = MaterialTheme.typography.labelMedium)
    }
}
