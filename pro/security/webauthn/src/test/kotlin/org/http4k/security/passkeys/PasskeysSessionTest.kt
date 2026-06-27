/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.security.passkeys

import com.natpryce.hamkrest.and
import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.http4k.connect.model.Base64UriBlob
import org.http4k.core.Method.GET
import org.http4k.core.Method.POST
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status.Companion.OK
import org.http4k.core.Status.Companion.SEE_OTHER
import org.http4k.core.Status.Companion.UNAUTHORIZED
import org.http4k.core.Uri
import org.http4k.core.cookie.cookie
import org.http4k.core.cookie.cookies
import org.http4k.core.then
import org.http4k.core.with
import org.http4k.filter.ClientFilters
import org.http4k.hamkrest.hasBody
import org.http4k.hamkrest.hasStatus
import org.http4k.lens.Header.LOCATION
import org.http4k.lens.RequestKey
import org.http4k.routing.bind
import org.http4k.routing.routes
import org.http4k.security.passkeys.model.AuthenticationOptions
import org.http4k.security.passkeys.model.PasskeyUser
import org.http4k.security.passkeys.model.RegistrationOptions
import org.http4k.security.passkeys.model.RelyingParty
import org.http4k.security.passkeys.testing.InMemoryPasskeyPersistence
import org.http4k.security.passkeys.testing.InsecureCookieBasedPrincipals
import org.http4k.security.passkeys.testing.InsecurePasskeyVerifier
import org.http4k.security.passkeys.util.PasskeysJson.json
import org.junit.jupiter.api.Test

class PasskeysSessionTest {
    private val rp = RelyingParty("example.com", "Example", Uri.of("https://example.com"))
    private val persistence = InMemoryPasskeyPersistence()
    private val contextKey = RequestKey.required<Base64UriBlob>("handle")
    private val session = InsecureCookieBasedPrincipals("http4k", contextKey)
    private fun userFor(handle: Base64UriBlob) = PasskeyUser(handle, "alice", "alice")
    private val toLogin = { req: Request -> Response(SEE_OTHER).with(LOCATION of Uri.of("/login?next=${req.uri.path}")) }
    private val passkeys = Passkeys.onTopOfExistingLogin(rp, InsecurePasskeyVerifier(), persistence, session, ::userFor, toLogin)
    private val device = FakePasskeyAuthenticator()
    private val handle = Base64UriBlob.randomHandle()

    private val app = routes(
        "/login" bind GET to { session.write(handle, Response(OK)) },
        "/passkeys" bind passkeys.routes,
        "/passkeys/logout" bind GET to passkeys.logout,
        "/protected" bind GET to passkeys.authFilter.then { Response(OK).body("top secret") }
    )

    private val browser = ClientFilters.Cookies().then(app)

    private fun req(method: org.http4k.core.Method, path: String) = Request(method, "https://example.com$path")

    private fun registerAndAuthenticate() {
        browser(req(GET, "/login"))   // registration is flow #2: must already be logged in
        val regOptions = browser(req(POST, "/passkeys/register/options")).json<RegistrationOptions>()
        browser(req(POST, "/passkeys/register").json(device.register(regOptions)))
        val authOptions = browser(req(POST, "/passkeys/authenticate/options")).json<AuthenticationOptions>()
        browser(req(POST, "/passkeys/authenticate").json(device.authenticate(authOptions)))
    }

    @Test
    fun `protected resource redirects to login when unauthenticated`() {
        val response = browser(req(GET, "/protected"))
        assertThat(response, hasStatus(SEE_OTHER))
        assertThat(response.header("Location"), equalTo("/login?next=/protected"))
    }

    @Test
    fun `protected resource is served once authenticated`() {
        registerAndAuthenticate()
        assertThat(browser(req(GET, "/protected")), hasStatus(OK).and(hasBody("top secret")))
    }

    @Test
    fun `logout clears the session`() {
        registerAndAuthenticate()
        browser(req(GET, "/passkeys/logout"))
        assertThat(browser(req(GET, "/protected")), hasStatus(SEE_OTHER))
    }

    @Test
    fun `authFilter populates the principal from the session for downstream handlers`() {
        val sessionCookie = session.write(handle, Response(OK)).cookies().first()
        val handler = passkeys.authFilter.then { Response(OK).body(contextKey(it).value) }
        val response = handler(req(GET, "/x").cookie(sessionCookie))
        assertThat(response, hasStatus(OK).and(hasBody(handle.value)))
    }

    @Test
    fun `authFilter defaults to 401 when no onUnauthenticated supplied`() {
        val pk = Passkeys.onTopOfExistingLogin(rp, InsecurePasskeyVerifier(), persistence, session, ::userFor)
        val handler = pk.authFilter.then { Response(OK) }
        assertThat(handler(req(GET, "/x")), hasStatus(UNAUTHORIZED))
    }

    @Test
    fun `passwordless signup registers and logs in with no prior session`() {
        val newUser = Base64UriBlob.randomHandle()
        val signup = Passkeys.passwordless(rp, InsecurePasskeyVerifier(), persistence, session,
            user = { PasskeyUser(newUser, "bob", "Bob") })   // identity from the (signup) request, no session
        val app = ClientFilters.Cookies().then(routes(
            "/passkeys" bind signup.routes,
            "/protected" bind GET to signup.authFilter.then { Response(OK).body("secret") }
        ))
        val regOptions = app(req(POST, "/passkeys/register/options")).json<RegistrationOptions>()  // no login first
        assertThat(app(req(POST, "/passkeys/register").json(device.register(regOptions))), hasStatus(OK))
        assertThat(app(req(GET, "/protected")), hasStatus(OK).and(hasBody("secret")))   // auto-logged-in
    }
}
