/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.security.passkeys.model

import org.http4k.connect.model.Base64UriBlob

/**
 * A WebAuthn PublicKeyCredentialDescriptor: a credential [id] plus optional [transports] hints that help the
 * browser locate the authenticator (eg. `internal`, `usb`, `hybrid`). [transports] is a free list of strings
 * (not an enum) so unknown/future transports pass through unharmed.
 */
data class CredentialDescriptor(
    val id: Base64UriBlob,
    val transports: List<String> = emptyList(),
    val type: String = "public-key"
)
