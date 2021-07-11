package org.http4k.filter

import com.natpryce.hamkrest.assertion.assertThat
import org.http4k.core.Credentials
import org.http4k.core.Method.GET
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status.Companion.OK
import org.http4k.core.Status.Companion.UNAUTHORIZED
import org.http4k.core.then
import org.http4k.hamkrest.hasHeader
import org.http4k.hamkrest.hasStatus
import org.http4k.security.digest.DigestMode
import org.http4k.security.digest.Qop.Auth
import org.junit.jupiter.api.Test

class DigestAuthTest {

    companion object {
        private val passwordLookup: (String) -> String? = { if (it == "admin") "password" else null }
        private const val realm = "http4k"
    }

    @Test
    fun `invalid credentials`() {
        val handler = ServerFilters.DigestAuth(realm, passwordLookup)
            .then { Response(OK) }
        val response = ClientFilters.DigestAuth(Credentials("admin", "hunter2")).then(handler)(Request(GET, "/"))

        assertThat(response, hasStatus(UNAUTHORIZED))
        assertThat(response, !hasHeader("WWW-Authenticate", ""))
    }

    @Test
    fun `valid credentials with Auth Qop`() {
        val handler =
            ServerFilters.DigestAuth(realm, passwordLookup, listOf(Auth)).then { Response(OK) }
        val response = ClientFilters.DigestAuth(Credentials("admin", "password")).then(handler)(Request(GET, "/"))


        assertThat(response, hasStatus(OK))
        assertThat(response, !hasHeader("WWW-Authenticate", ""))
    }

//    @Test TODO not fully implemented
//    fun `valid credentials with Auth-Int Qop`() {
//        val handler = ServerFilters.DigestAuth(realm, passwordLookup, qop = listOf(Qop.AuthInt)).then { Response(Status.OK) }
//        val response = ClientFilters.DigestAuth(Credentials("admin", "password")).then(handler)(Request(Method.GET, "/"))
//
//        assertThat(response.status, equalTo(Status.OK))
//        assertThat(response.header("WWW-Authenticate"), isNullOrBlank)
//    }

    @Test
    fun `valid credentials with no qop`() {
        val handler = ServerFilters.DigestAuth(realm, passwordLookup, emptyList()).then { Response(OK) }
        val response = ClientFilters.DigestAuth(Credentials("admin", "password"))
            .then(handler)(Request(GET, "/"))

        assertThat(response, hasStatus(OK))
        assertThat(response, !hasHeader("WWW-Authenticate"))
    }

    @Test
    fun `valid credentials in proxy mode`() {
        val handler = ServerFilters.DigestAuth(realm, passwordLookup, digestMode = DigestMode.Proxy).then { Response(OK) }
        val response = ClientFilters.DigestAuth(Credentials("admin", "password"), digestMode = DigestMode.Proxy)
            .then(handler)(Request(GET, "/"))

        assertThat(response, hasStatus(OK))
    }
}
