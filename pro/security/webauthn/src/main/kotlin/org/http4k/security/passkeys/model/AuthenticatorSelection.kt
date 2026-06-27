/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.security.passkeys.model

data class AuthenticatorSelection(
    val residentKey: ResidentKey = ResidentKey.PREFERRED,
    val userVerification: UserVerification = UserVerification.PREFERRED,
    val authenticatorAttachment: AuthenticatorAttachment? = null
)
