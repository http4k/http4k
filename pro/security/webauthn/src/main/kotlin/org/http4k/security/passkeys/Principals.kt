/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.security.passkeys

import org.http4k.connect.model.Base64UriBlob
import org.http4k.core.Request
import org.http4k.core.Response

/**
 * Links a verified passkey (or any login) to the app's HTTP session: it writes the authenticated user
 * handle onto a response and reads it back off later requests.
 */
interface Principals {
    /**
     * Establish the session for a verified user (eg. set a cookie) on the auth-success response.
     */
    fun write(userHandle: Base64UriBlob, response: Response): Response

    /**
     * Resolve the principal for a request:
     */
    fun read(request: Request): Principal

    /**
     * Drop the session (logout).
     */
    fun clear(response: Response): Response
}

sealed interface Principal {
    data class Known(val request: Request, val userHandle: Base64UriBlob) : Principal
    data object Anonymous : Principal
}
