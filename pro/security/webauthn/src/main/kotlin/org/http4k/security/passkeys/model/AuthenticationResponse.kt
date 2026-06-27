/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.security.passkeys.model

import org.http4k.connect.model.Base64UriBlob

data class AuthenticationResponse(
    val credentialId: Base64UriBlob,
    val clientDataJSON: Base64UriBlob,
    val authenticatorData: Base64UriBlob,
    val signature: Base64UriBlob
)
