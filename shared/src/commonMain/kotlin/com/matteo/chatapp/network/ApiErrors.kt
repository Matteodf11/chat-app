package com.matteo.chatapp.network

import io.ktor.client.call.body
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import io.ktor.http.isSuccess
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

class ApiException(
    message: String,
    val statusCode: Int
) : Exception(message)

suspend inline fun <reified T> HttpResponse.bodyOrApiError(): T {
    if (status.isSuccess()) return body()
    throw ApiException(parseApiError(bodyAsText(), status), status.value)
}

suspend fun HttpResponse.requireApiSuccess() {
    if (status.isSuccess()) return
    throw ApiException(parseApiError(bodyAsText(), status), status.value)
}

fun parseApiError(rawBody: String, status: HttpStatusCode): String {
    val body = runCatching {
        Json.parseToJsonElement(rawBody).jsonObject
    }.getOrNull()
    val fields = body.orEmpty()
        .mapNotNull { (key, value) ->
            (value as? JsonPrimitive)?.contentOrNull
                ?.takeIf(String::isNotBlank)?.let { key to it }
        }.toMap()

    if (status == HttpStatusCode.BadRequest) {
        val details = (body?.get("details") as? JsonObject)?.values
            ?.mapNotNull { (it as? JsonPrimitive)?.contentOrNull }
            ?.filter(String::isNotBlank)?.distinct().orEmpty()
        if (details.isNotEmpty()) return details.joinToString("\n")
    }
    fields["message"]?.let { return it }
    fields["error"]?.let { return it }

    val validationFields = setOf("username", "email", "password")
    val orderedMessages = listOf("username", "email", "password")
        .mapNotNull(fields::get)
        .plus(fields.filterKeys { it !in validationFields }.values)
        .distinct()

    return orderedMessages.takeIf { it.isNotEmpty() }?.joinToString("\n")
        ?: "El servidor rechazó la solicitud (${status.value})"
}
