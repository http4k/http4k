/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.security.passkeys.model

import org.http4k.connect.model.Base64UriBlob

data class RegisteredCredential(
    val credentialId: Base64UriBlob,
    val publicKey: Base64UriBlob,
    val signCount: Long,
    val userHandle: Base64UriBlob,
    val backupEligible: Boolean = false,
    val backupState: Boolean = false,
    val transports: List<String> = emptyList(),
    val discoverable: Boolean? = null
)
