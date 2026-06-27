/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.security.passkeys.testing

import dev.forkhandles.result4k.Result
import dev.forkhandles.result4k.Success
import org.http4k.security.passkeys.PasskeyVerifier
import org.http4k.security.passkeys.model.AuthenticationOptions
import org.http4k.security.passkeys.model.AuthenticationResponse
import org.http4k.security.passkeys.model.AuthenticationVerdict
import org.http4k.security.passkeys.model.PasskeyError
import org.http4k.security.passkeys.model.RegisteredCredential
import org.http4k.security.passkeys.model.RegistrationOptions
import org.http4k.security.passkeys.model.RegistrationResponse

class InsecurePasskeyVerifier : PasskeyVerifier {
    override fun verifyRegistration(
        options: RegistrationOptions,
        response: RegistrationResponse
    ): Result<RegisteredCredential, PasskeyError> =
        Success(
            RegisteredCredential(
                response.credentialId, response.attestationObject, 0, options.user.handle,
                transports = response.transports,
                discoverable = response.clientExtensionResults.credProps?.rk
            )
        )

    override fun verifyAuthentication(
        options: AuthenticationOptions,
        response: AuthenticationResponse,
        stored: RegisteredCredential
    ): Result<AuthenticationVerdict, PasskeyError> = Success(AuthenticationVerdict(stored.signCount + 1, stored.backupState))
}
