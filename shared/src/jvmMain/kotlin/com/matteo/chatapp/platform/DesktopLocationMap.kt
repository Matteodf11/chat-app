package com.matteo.chatapp.platform

import com.sun.net.httpserver.HttpExchange
import com.sun.net.httpserver.HttpServer
import java.net.InetSocketAddress
import java.net.URI
import java.util.UUID
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean
import kotlinx.coroutines.CompletableDeferred

/** Short-lived loopback bridge: selects a point, never sends a chat message. */
internal class DesktopLocationMap(initial: SharedLocation?, locate: Boolean) : AutoCloseable {
    val result = CompletableDeferred<SharedLocation?>()
    private val closed = AtomicBoolean(false)
    private val token = UUID.randomUUID().toString()
    private val executor = Executors.newFixedThreadPool(2) { task ->
        Thread(task, "chimera-location-map").apply { isDaemon = true }
    }
    private val server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
    private val origin = "http://127.0.0.1:${server.address.port}"
    val url: URI = URI("$origin/$token/")
    private val html: ByteArray

    init {
        try {
            html = checkNotNull(javaClass.getResourceAsStream("/location-map.html"))
                .use { it.bufferedReader(Charsets.UTF_8).readText() }
                .replace("__LATITUDE__", (initial?.latitude ?: 40.0).toString())
                .replace("__LONGITUDE__", (initial?.longitude ?: -4.0).toString())
                .replace("__HAS_SELECTION__", (initial != null).toString())
                .replace("__LOCATE__", locate.toString())
                .replace("__TOKEN__", token).toByteArray(Charsets.UTF_8)
            server.executor = executor
            server.createContext("/") { exchange ->
                try { handle(exchange) } finally { exchange.close() }
            }
            server.start()
        } catch (failure: Throwable) {
            server.stop(0); executor.shutdownNow()
            throw failure
        }
    }

    @Synchronized
    private fun handle(exchange: HttpExchange) {
        if (exchange.requestHeaders.getFirst("Host") != url.authority) {
            respond(exchange, 403, "Acceso no permitido"); return
        }
        val path = exchange.requestURI.path
        if (exchange.requestMethod == "GET" && path == "/$token/") {
            exchange.responseHeaders.set("Content-Type", "text/html; charset=utf-8")
            exchange.responseHeaders.set("Referrer-Policy", "strict-origin-when-cross-origin")
            exchange.responseHeaders.set("X-Frame-Options", "DENY")
            exchange.responseHeaders.set("X-Content-Type-Options", "nosniff")
            exchange.responseHeaders.set("Cache-Control", "no-store")
            exchange.sendResponseHeaders(200, html.size.toLong())
            exchange.responseBody.use { it.write(html) }
            return
        }
        if (exchange.requestMethod != "POST" || path != "/$token/select") {
            respond(exchange, 404, "No encontrado"); return
        }
        if (exchange.requestHeaders.getFirst("Origin") != origin ||
            exchange.requestHeaders.getFirst("X-Chimera-Selection") != token) {
            respond(exchange, 403, "Acceso no permitido"); return
        }
        if (result.isCompleted) { respond(exchange, 409, "Selección terminada"); return }
        val length = exchange.requestHeaders.getFirst("Content-Length")?.toIntOrNull()
        if (length == null || length !in 1..128) { respond(exchange, 400, "Selección no válida"); return }
        val body = exchange.requestBody.readNBytes(length).toString(Charsets.UTF_8)
        val selection = if (body == "cancel") null else {
            val pair = body.split('|')
            val lat = pair.getOrNull(0)?.toDoubleOrNull()
            val lon = pair.getOrNull(1)?.toDoubleOrNull()
            if (pair.size != 2 || lat == null || lon == null) {
                respond(exchange, 400, "Selecciona un punto válido"); return
            }
            try { SharedLocation(lat, lon) } catch (_: IllegalArgumentException) {
                respond(exchange, 400, "Selecciona un punto válido"); return
            }
        }
        respond(exchange, 200, "OK")
        result.complete(selection)
    }

    private fun respond(exchange: HttpExchange, status: Int, text: String) {
        val bytes = text.toByteArray(Charsets.UTF_8)
        exchange.responseHeaders.set("Content-Type", "text/plain; charset=utf-8")
        exchange.responseHeaders.set("Cache-Control", "no-store")
        exchange.sendResponseHeaders(status, bytes.size.toLong())
        exchange.responseBody.use { it.write(bytes) }
    }

    override fun close() {
        if (!closed.compareAndSet(false, true)) return
        server.stop(0)
        executor.shutdownNow()
        result.cancel()
    }
}
