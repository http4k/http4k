/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.security.passkeys

import dev.forkhandles.result4k.Result
import org.http4k.security.passkeys.model.AuthenticationOptions
import org.http4k.security.passkeys.model.AuthenticationResponse
import org.http4k.security.passkeys.model.AuthenticationVerdict
import org.http4k.security.passkeys.model.PasskeyError
import org.http4k.security.passkeys.model.RegisteredCredential
import org.http4k.security.passkeys.model.RegistrationOptions
import org.http4k.security.passkeys.model.RegistrationResponse

/**
 * The security-critical extension point. Implementations decode CBOR/COSE, validate
 * attestation and check signatures using a library (eg. webauthn4j, Yubico).
 */
interface PasskeyVerifier {
    fun verifyRegistration(
        options: RegistrationOptions,
        response: RegistrationResponse
    ): Result<RegisteredCredential, PasskeyError>

    fun verifyAuthentication(
        options: AuthenticationOptions,
        response: AuthenticationResponse,
        stored: RegisteredCredential
    ): Result<AuthenticationVerdict, PasskeyError>
}
