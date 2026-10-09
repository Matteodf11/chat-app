package com.matteo.chatapp.network

import io.ktor.client.HttpClient
import io.ktor.client.plugins.HttpSend
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.plugin
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.plugins.websocket.WebSockets
import io.ktor.client.request.bearerAuth
import io.ktor.serialization.kotlinx.json.json
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import kotlinx.serialization.json.Json

val BASE_URL = "https://chat-backend-gnj6.onrender.com"

fun createHttpClient(enableSessionRecovery: Boolean = true): HttpClient {
    val client = HttpClient {
        install(HttpTimeout) {
                requestTimeoutMillis = 60_000
                connectTimeoutMillis = 15_000
                socketTimeoutMillis = 60_000
        }
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                isLenient = true
            })
        }
        install(Logging) {
            level = LogLevel.INFO
            sanitizeHeader { it == io.ktor.http.HttpHeaders.Authorization }
        }
        install(WebSockets)
    }
    if (enableSessionRecovery) {
        client.plugin(HttpSend).intercept { request ->
            val firstCall = execute(request)
            val hasBearerToken = request.headers[HttpHeaders.Authorization]
                ?.startsWith("Bearer ") == true
            val isLegacyEmptyForbidden = firstCall.response.status == HttpStatusCode.Forbidden &&
                firstCall.response.headers[HttpHeaders.ContentType] == null
            val sessionRejected = firstCall.response.status == HttpStatusCode.Unauthorized ||
                isLegacyEmptyForbidden

            if (!hasBearerToken || !sessionRejected) return@intercept firstCall

            val failedToken = request.headers[HttpHeaders.Authorization]
                ?.removePrefix("Bearer ")
            val renewedToken = SessionRefresher.recoverAfterRejectedToken(failedToken)
                ?: return@intercept firstCall

            request.headers.remove(HttpHeaders.Authorization)
            request.bearerAuth(renewedToken)
            execute(request)
        }
    }
    return client
}
