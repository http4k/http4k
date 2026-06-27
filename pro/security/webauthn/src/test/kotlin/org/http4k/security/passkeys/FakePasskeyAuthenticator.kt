/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.security.passkeys

import org.http4k.connect.model.Base64UriBlob
import org.http4k.security.passkeys.model.AuthenticationOptions
import org.http4k.security.passkeys.model.AuthenticationResponse
import org.http4k.security.passkeys.model.RegistrationOptions
import org.http4k.security.passkeys.model.RegistrationResponse
import java.security.SecureRandom

class FakePasskeyAuthenticator {
    private val random = SecureRandom()
    private val credentialIds = mutableListOf<Base64UriBlob>()
    private val placeholder = Base64UriBlob.encode("{}")

    fun register(options: RegistrationOptions): RegistrationResponse =
        Base64UriBlob.encode(ByteArray(16).also(random::nextBytes))
            .also { credentialIds += it }
            .let { RegistrationResponse(it, placeholder, placeholder) }

    fun authenticate(options: AuthenticationOptions): AuthenticationResponse {
        val credentialId = options.allowCredentials.map { it.id }.ifEmpty { credentialIds }.first()
        return AuthenticationResponse(credentialId, placeholder, placeholder, placeholder)
    }
}
