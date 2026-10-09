package com.matteo.chatapp.network

import io.ktor.http.HttpStatusCode
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ApiErrorsTest {
    @Test
    fun readsStructuredBackendErrorWithNestedDetails() {
        assertEquals(
            "Credenciales no válidas",
            parseApiError(
                """{"status":401,"error":"No autorizado","message":"Credenciales no válidas","details":{}}""",
                HttpStatusCode.Unauthorized
            )
        )
    }

    @Test
    fun readsNestedFieldValidationErrors() {
        assertEquals(
            "Email no válido",
            parseApiError(
                """{"error":"Error de validación","message":"Datos inválidos","details":{"email":"Email no válido"}}""",
                HttpStatusCode.BadRequest
            )
        )
    }


    @Test
    fun readsBackendErrorMessage() {
        assertEquals(
            "El email ya esta en uso",
            parseApiError("""{"error":"El email ya esta en uso"}""", HttpStatusCode.BadRequest)
        )
    }

    @Test
    fun combinesValidationFieldsInsteadOfTryingToReadAuthResponse() {
        val message = parseApiError(
            """{"password":"Contraseña demasiado corta","email":"Email no válido"}""",
            HttpStatusCode.BadRequest
        )

        assertTrue(message.contains("Contraseña demasiado corta"))
        assertTrue(message.contains("Email no válido"))
    }
}
