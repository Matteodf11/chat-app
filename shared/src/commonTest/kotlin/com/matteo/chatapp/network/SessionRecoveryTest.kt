package com.matteo.chatapp.network

import kotlinx.coroutines.CancellationException
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SessionRecoveryTest {
    @Test
    fun onlyExplicitAuthenticationRejectionsInvalidateASession() {
        assertTrue(isSessionRejected(ApiException("Expired", 401)))
        assertTrue(isSessionRejected(ApiException("Revoked", 403)))
        assertFalse(isSessionRejected(ApiException("Server unavailable", 503)))
        assertFalse(isSessionRejected(ApiException("Too many requests", 429)))
        assertFalse(isSessionRejected(Exception("Connection lost")))
        assertFalse(isSessionRejected(CancellationException()))
    }
}
