/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.security.passkeys.model

data class WebAuthnPolicy(
    val pubKeyCredParams: List<PubKeyCredParam> = defaultPubKeyCredParams,
    val authenticatorSelection: AuthenticatorSelection = AuthenticatorSelection(),
    val attestation: Attestation = Attestation.NONE,
    val timeout: Long = 300_000
)
