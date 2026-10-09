package com.matteo.chatapp.network

/** Connectivity errors, server outages and cancellations never invalidate saved credentials. */
internal fun isSessionRejected(error: Throwable): Boolean =
    error is ApiException && error.statusCode in setOf(401, 403)
