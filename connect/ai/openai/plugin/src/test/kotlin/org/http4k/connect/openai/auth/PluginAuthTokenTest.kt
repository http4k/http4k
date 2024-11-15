package org.http4k.connect.openai.auth

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.http4k.connect.openai.auth.PluginAuthToken.Basic
import org.http4k.connect.openai.auth.PluginAuthToken.Bearer
import org.http4k.core.Credentials
import org.http4k.core.Method.GET
import org.http4k.core.Request
import org.http4k.core.RequestContexts
import org.http4k.core.Response
import org.http4k.core.Status.Companion.OK
import org.http4k.core.Status.Companion.UNAUTHORIZED
import org.http4k.core.then
import org.http4k.core.with
import org.http4k.filter.ServerFilters.InitialiseRequestContext
import org.http4k.filter.debug
import org.http4k.lens.Header
import org.http4k.lens.RequestContextKey.required
import org.http4k.security.AccessToken
import org.junit.jupiter.api.Test

class PluginAuthTokenTest {

    private val validCreds = Credentials("user", "password")
    private val validToken = AccessToken("foobar")
    private val store = RequestContexts()

    @Test
    fun `test basic auth`() {
        val validRequest = Request(GET, "")
            .with(Header.AUTHORIZATION_BASIC of validCreds)

        Basic("realm", required(store)) { it.takeIf { it == validCreds } }
            .assertAuthsOk(validRequest)

        Basic("realm") { it == validCreds }
            .assertAuthsOk(validRequest)
    }

    @Test
    fun `test bearer auth`() {
        val validRequest = Request(GET, "")
            .header("Authorization", "Bearer " + validToken.value)

        Bearer(required(store)) { it.takeIf { it == validToken } }.assertAuthsOk(validRequest)

        Bearer { it == validToken }.assertAuthsOk(validRequest)
    }

    private fun PluginAuthToken.assertAuthsOk(req: Request) {
        val withRC = InitialiseRequestContext(store)
            .then(securityFilter)
            .then { Response(OK) }.debug()

        assertThat(withRC(req.removeHeaders()).status, equalTo(UNAUTHORIZED))
        assertThat(withRC(req).status, equalTo(OK))
    }
}
