package com.matteo.chatapp.ui.components

import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Flip
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.PhotoCamera
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.matteo.chatapp.platform.PickedAttachmentFile

@Composable
internal fun PhotoReviewDialog(
    files: List<PickedAttachmentFile>, initialComment: String, camera: Boolean, working: Boolean, error: String?,
    onRemove: (Int) -> Unit, onRetake: () -> Unit, onFlip: (Int) -> Unit, onSend: (String) -> Unit, onDismiss: () -> Unit
) {
    var selected by remember { mutableStateOf(0) }
    var comment by remember { mutableStateOf(initialComment) }
    val index = selected.coerceIn(0, files.lastIndex)
    Dialog(onDismissRequest = { if (!working) onDismiss() }, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(Modifier.widthIn(max = 600.dp).fillMaxWidth().padding(12.dp).imePadding(),
            shape = RoundedCornerShape(24.dp)) {
            Column(Modifier.verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(if (files.size == 1) "Revisar foto" else "Revisar fotos · ${index+1}/${files.size}", Modifier.weight(1f))
                    IconButton(enabled = !working, onClick = onDismiss) { Icon(Icons.Rounded.Close, "Cancelar") }
                }
                PickedPhoto(files[index], 1024, Modifier.fillMaxWidth().height(260.dp))
                if (files.size > 1) LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    itemsIndexed(files) { item, file ->
                        PickedPhoto(file, 128, Modifier.size(60.dp)
                            .border(if (item == index) 2.dp else 0.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(8.dp))
                            .clickable(enabled = !working) { selected = item })
                    }
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    if (camera) TextButton(enabled = !working, contentPadding = PaddingValues(horizontal = 8.dp), onClick = onRetake) {
                        Icon(Icons.Rounded.PhotoCamera, null, Modifier.size(18.dp)); Spacer(Modifier.width(4.dp)); Text("Repetir")
                    }
                    if (camera) TextButton(enabled = !working, contentPadding = PaddingValues(horizontal = 8.dp), onClick = { onFlip(index) }) {
                        Icon(Icons.Rounded.Flip, null, Modifier.size(18.dp)); Spacer(Modifier.width(4.dp)); Text("Voltear")
                    }
                    TextButton(enabled = !working, contentPadding = PaddingValues(horizontal = 8.dp), onClick = { onRemove(index) }) {
                        Icon(Icons.Rounded.DeleteOutline, null, Modifier.size(18.dp)); Spacer(Modifier.width(4.dp)); Text("Quitar")
                    }
                }
                OutlinedTextField(comment, { comment = it.take(4000) }, enabled = !working,
                    placeholder = { Text("Añade un comentario") }, modifier = Modifier.fillMaxWidth(), maxLines = 3)
                error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                Button(enabled = !working, onClick = { onSend(comment) }, modifier = Modifier.fillMaxWidth()) {
                    if (working) CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                    else Text(if (files.size == 1) "Enviar foto" else "Enviar ${files.size} fotos")
                }
            }
        }
    }
}

@Composable
private fun PickedPhoto(file: PickedAttachmentFile, maxEdge: Int, modifier: Modifier) {
    var loaded by remember(file, maxEdge) { mutableStateOf(false) }
    val image by produceState<ImageBitmap?>(null, file, maxEdge) {
        value = decodePickedImage(file.bytes, maxEdge)
        loaded = true
    }
    Box(modifier.background(MaterialTheme.colorScheme.surfaceVariant), contentAlignment = Alignment.Center) {
        if (image != null) Image(image!!, "Vista previa de ${file.fileName}", Modifier.fillMaxSize(), contentScale = ContentScale.Fit)
        else if (!loaded) CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
        else Text("Vista previa no disponible")
    }
}
