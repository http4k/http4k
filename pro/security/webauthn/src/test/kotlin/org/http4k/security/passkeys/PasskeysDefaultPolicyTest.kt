/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.security.passkeys

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.containsSubstring
import org.http4k.connect.model.Base64UriBlob
import org.http4k.core.Method.POST
import org.http4k.core.Request
import org.http4k.core.Uri
import org.http4k.lens.RequestKey
import org.http4k.security.passkeys.model.PasskeyUser
import org.http4k.security.passkeys.model.RelyingParty
import org.http4k.security.passkeys.testing.InMemoryPasskeyPersistence
import org.http4k.security.passkeys.testing.InsecureCookieBasedPrincipals
import org.http4k.security.passkeys.testing.InsecurePasskeyVerifier
import org.junit.jupiter.api.Test

class PasskeysDefaultPolicyTest {
    private val rp = RelyingParty("example.com", "Example", setOf(Uri.of("https://example.com")))
    private val handle = Base64UriBlob.of("YWxpY2UtaGFuZGxl")

    private val passwordless = Passkeys.passwordless(
        rp, InsecurePasskeyVerifier(), InMemoryPasskeyPersistence(),
        InsecureCookieBasedPrincipals("http4k", RequestKey.required<Base64UriBlob>("handle")),
        user = { PasskeyUser(handle, "alice", "Alice") }
    )

    @Test
    fun `passwordless defaults to user verification required`() {
        val response = passwordless.routes(Request(POST, "https://example.com/authenticate/options"))

        assertThat(response.bodyString(), containsSubstring("\"userVerification\":\"required\""))
    }
}
