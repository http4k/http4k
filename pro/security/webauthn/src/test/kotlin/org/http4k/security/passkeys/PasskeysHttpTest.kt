/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.security.passkeys

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import dev.forkhandles.result4k.Failure
import org.http4k.connect.model.Base64UriBlob
import org.http4k.core.Method.GET
import org.http4k.core.Method.POST
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status.Companion.BAD_REQUEST
import org.http4k.core.Status.Companion.OK
import org.http4k.core.Status.Companion.UNAUTHORIZED
import org.http4k.core.Uri
import org.http4k.core.then
import org.http4k.filter.ClientFilters
import org.http4k.hamkrest.hasStatus
import org.http4k.lens.RequestKey
import org.http4k.routing.bind
import org.http4k.routing.routes
import org.http4k.security.passkeys.model.AuthenticationOptions
import org.http4k.security.passkeys.model.AuthenticationResponse
import org.http4k.security.passkeys.model.PasskeyError.BadSignature
import org.http4k.security.passkeys.model.PasskeyUser
import org.http4k.security.passkeys.model.RegisteredCredential
import org.http4k.security.passkeys.model.RegistrationOptions
import org.http4k.security.passkeys.model.RegistrationResponse
import org.http4k.security.passkeys.model.RelyingParty
import org.http4k.security.passkeys.testing.InMemoryPasskeyPersistence
import org.http4k.security.passkeys.testing.InsecureCookieBasedPrincipals
import org.http4k.security.passkeys.testing.InsecurePasskeyVerifier
import org.http4k.security.passkeys.util.AuthenticationResult
import org.http4k.security.passkeys.util.PasskeysJson.json
import org.junit.jupiter.api.Test

class PasskeysHttpTest {
    private val rp = RelyingParty("example.com", "Example", Uri.of("https://example.com"))
    private val persistence = InMemoryPasskeyPersistence()
    private val contextKey = RequestKey.required<Base64UriBlob>("handle")
    private val session = InsecureCookieBasedPrincipals("http4k", contextKey)
    private fun userFor(handle: Base64UriBlob) = PasskeyUser(handle, "alice", "alice")
    private val passkeys = Passkeys.onTopOfExistingLogin(rp, InsecurePasskeyVerifier(), persistence, session, ::userFor)
    private val device = FakePasskeyAuthenticator()

    // registration is "add a passkey while logged in" - the user must already have a session
    private val handle = Base64UriBlob.randomHandle()
    private val app = routes(
        "/login" bind GET to { session.write(handle, Response(OK)) },
        "/passkeys" bind passkeys.routes
    )
    private val browser = ClientFilters.Cookies().then(app)

    private fun req(method: org.http4k.core.Method, path: String) = Request(method, "https://example.com$path")

    private fun registerAPasskey() {
        browser(req(GET, "/login"))
        val options = browser(req(POST, "/passkeys/register/options")).json<RegistrationOptions>()
        assertThat(browser(req(POST, "/passkeys/register").json(device.register(options))), hasStatus(OK))
    }

    @Test
    fun `register then authenticate over http`() {
        registerAPasskey()

        val options = browser(req(POST, "/passkeys/authenticate/options")).json<AuthenticationOptions>()
        val result = browser(req(POST, "/passkeys/authenticate").json(device.authenticate(options)))

        assertThat(result, hasStatus(OK))
        assertThat(result.json<AuthenticationResult>().userHandle, equalTo(handle))
    }

    @Test
    fun `authenticating an unknown credential is unauthorized`() {
        registerAPasskey()
        // a second device that mints a credential the server never stored
        val stranger = FakePasskeyAuthenticator()
        stranger.register(browser(req(POST, "/passkeys/register/options")).json<RegistrationOptions>())

        val options = browser(req(POST, "/passkeys/authenticate/options")).json<AuthenticationOptions>()
        val result = browser(req(POST, "/passkeys/authenticate").json(stranger.authenticate(options)))

        assertThat(result, hasStatus(UNAUTHORIZED))
    }

    @Test
    fun `registration is rejected when the verifier fails`() {
        val rejecting = object : PasskeyVerifier {
            override fun verifyRegistration(options: RegistrationOptions, response: RegistrationResponse) =
                Failure(BadSignature)

            override fun verifyAuthentication(options: AuthenticationOptions, response: AuthenticationResponse, stored: RegisteredCredential) =
                Failure(BadSignature)
        }
        val app = ClientFilters.Cookies().then(
            routes(
                "/login" bind GET to { session.write(handle, Response(OK)) },
                "/passkeys" bind Passkeys.onTopOfExistingLogin(rp, rejecting, persistence, session, ::userFor).routes
            )
        )
        app(req(GET, "/login"))
        val options = app(req(POST, "/passkeys/register/options")).json<RegistrationOptions>()
        assertThat(app(req(POST, "/passkeys/register").json(device.register(options))), hasStatus(BAD_REQUEST))
    }
}
