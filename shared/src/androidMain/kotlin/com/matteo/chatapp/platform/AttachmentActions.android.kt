package com.matteo.chatapp.platform

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.LocationManager
import android.net.Uri
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.location.LocationManagerCompat
import androidx.core.os.CancellationSignal
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

internal object AndroidAttachmentActions {
    private var saveLauncher: ActivityResultLauncher<String>? = null
    private var permissionLauncher: ActivityResultLauncher<Array<String>>? = null
    private var saveResult: ((Uri?) -> Unit)? = null
    private var permissionResult: ((Boolean) -> Unit)? = null

    fun initialize(activity: ComponentActivity) {
        saveLauncher = activity.registerForActivityResult(ActivityResultContracts.CreateDocument("*/*")) {
            val callback = saveResult; saveResult = null; callback?.invoke(it)
        }
        permissionLauncher = activity.registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
            val callback = permissionResult; permissionResult = null; callback?.invoke(it.values.any { granted -> granted })
        }
    }

    suspend fun destination(name: String): Uri? = suspendCancellableCoroutine { continuation ->
        check(saveResult == null && saveLauncher != null) { "Ya hay un guardado en curso" }
        saveResult = { if (continuation.isActive) continuation.resume(it) }
        continuation.invokeOnCancellation { saveResult = null }
        saveLauncher!!.launch(attachmentSaveName(name))
    }

    suspend fun permission(): Boolean {
        if (ContextCompat.checkSelfPermission(AndroidPlatform.context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) return true
        return suspendCancellableCoroutine { continuation ->
            check(permissionResult == null && permissionLauncher != null) { "Ya se está solicitando el permiso" }
            permissionResult = { if (continuation.isActive) continuation.resume(it) }
            continuation.invokeOnCancellation { permissionResult = null }
            permissionLauncher!!.launch(arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION))
        }
    }
}

actual suspend fun saveAttachment(url: String, fileName: String): Boolean {
    val uri = AndroidAttachmentActions.destination(fileName) ?: return false
    try {
        return withContext(Dispatchers.IO) {
            val bytes = downloadAttachmentBytes(url)
            AndroidPlatform.context.contentResolver.openOutputStream(uri, "wt")?.use { it.write(bytes) }
                ?: error("No se pudo guardar el archivo")
            true
        }
    } catch (e: Exception) {
        runCatching { AndroidPlatform.context.contentResolver.delete(uri, null, null) }
        throw e
    }
}

actual fun supportsCurrentLocation(): Boolean = true
actual fun supportsLocationMap(): Boolean = false
actual suspend fun chooseLocationOnMap(initial: SharedLocation?): SharedLocation? = null

actual fun openLocationSettings() {
    AndroidPlatform.context.startActivity(android.content.Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS)
        .addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK))
}

actual suspend fun currentLocation(): SharedLocation {
    check(AndroidAttachmentActions.permission()) { "Permiso de ubicación denegado. Puedes introducir las coordenadas." }
    val manager = AndroidPlatform.context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
    check(LocationManagerCompat.isLocationEnabled(manager)) { "Activa la ubicación del dispositivo e inténtalo de nuevo" }
    val fine = ContextCompat.checkSelfPermission(AndroidPlatform.context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
    val provider = listOfNotNull(LocationManager.NETWORK_PROVIDER, LocationManager.GPS_PROVIDER.takeIf { fine })
        .firstOrNull { manager.isProviderEnabled(it) } ?: error("No hay un proveedor de ubicación disponible")
    return withTimeout(25000) {
        suspendCancellableCoroutine { continuation ->
            val cancellation = CancellationSignal()
            continuation.invokeOnCancellation { cancellation.cancel() }
            LocationManagerCompat.getCurrentLocation(manager, provider, cancellation,
                ContextCompat.getMainExecutor(AndroidPlatform.context)) { location ->
                if (continuation.isActive) {
                    if (location == null) continuation.resumeWithException(IllegalStateException("No se pudo obtener la ubicación. Inténtalo al aire libre o introduce las coordenadas."))
                    else continuation.resume(SharedLocation(location.latitude, location.longitude))
                }
            }
        }
    }
}
