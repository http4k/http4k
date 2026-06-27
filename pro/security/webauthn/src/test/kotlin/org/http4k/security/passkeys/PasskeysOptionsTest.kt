/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.security.passkeys

import org.http4k.connect.model.Base64UriBlob
import org.http4k.core.Method.POST
import org.http4k.core.Request
import org.http4k.core.Uri
import org.http4k.lens.RequestKey
import org.http4k.security.passkeys.model.Attestation
import org.http4k.security.passkeys.model.AuthenticatorAttachment
import org.http4k.security.passkeys.model.AuthenticatorSelection
import org.http4k.security.passkeys.model.CredentialDescriptor
import org.http4k.security.passkeys.model.PasskeyUser
import org.http4k.security.passkeys.model.PubKeyCredParam
import org.http4k.security.passkeys.model.RelyingParty
import org.http4k.security.passkeys.model.ResidentKey
import org.http4k.security.passkeys.model.UserVerification
import org.http4k.security.passkeys.model.WebAuthnPolicy
import org.http4k.security.passkeys.testing.InMemoryPasskeyPersistence
import org.http4k.security.passkeys.testing.InsecureCookieBasedPrincipals
import org.http4k.security.passkeys.testing.InsecurePasskeyVerifier
import org.http4k.testing.Approver
import org.http4k.testing.JsonApprovalTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(JsonApprovalTest::class)
class PasskeysOptionsTest {
    private val rp =
        RelyingParty("example.com", "Example", setOf(Uri.of("https://example.com"), Uri.of("https://www.example.com")))
    private val handle = Base64UriBlob.of("YWxpY2UtaGFuZGxl")
    private val cred = Base64UriBlob.of("Y3JlZGVudGlhbC1pZA")

    private val policy = WebAuthnPolicy(
        pubKeyCredParams = listOf(PubKeyCredParam(-7)),
        authenticatorSelection = AuthenticatorSelection(
            ResidentKey.REQUIRED,
            UserVerification.REQUIRED,
            AuthenticatorAttachment.PLATFORM
        ),
        attestation = Attestation.DIRECT,
        timeout = 120_000
    )

    private val passkeys = Passkeys.passwordless(
        rp, InsecurePasskeyVerifier(), InMemoryPasskeyPersistence(),
        InsecureCookieBasedPrincipals("http4k", RequestKey.required<Base64UriBlob>("handle")),
        user = { PasskeyUser(handle, "alice", "Alice") },
        policy = policy,
        allowedCredentialsFor = { listOf(CredentialDescriptor(cred, listOf("internal", "hybrid"))) },
        newChallenge = { Base64UriBlob.of("Y2hhbGxlbmdlLTEyMw") }
    )

    private fun req(path: String) = Request(POST, "https://example.com$path")

    @Test
    fun `registration options carry the server-issued policy and multiple origins`(approver: Approver) {
        approver.assertApproved(passkeys.routes(req("/register/options")))
    }

    @Test
    fun `authentication options carry allowCredentials and the policy`(approver: Approver) {
        approver.assertApproved(passkeys.routes(req("/authenticate/options")))
    }
}
