package org.http4k.filter.auth.digest

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import com.natpryce.hamkrest.isNullOrBlank
import org.http4k.core.Credentials
import org.http4k.core.Method
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status
import org.http4k.core.then
import org.http4k.filter.ClientFilters
import org.http4k.filter.ServerFilters
import org.junit.jupiter.api.Test

class DigestAuthTest {

    companion object {
        private val passwordLookup: (String) -> String? = { if (it == "admin") "password" else null }
        private const val realm = "http4k"
    }

    @Test
    fun `invalid credentials`() {
        val handler = ServerFilters.DigestAuth(realm, passwordLookup).then { Response(Status.OK) }
        val response = ClientFilters.DigestAuth(Credentials("admin", "hunter2")).then(handler)(Request(Method.GET, "/"))

        assertThat(response.status, equalTo(Status.UNAUTHORIZED))
        assertThat(response.header("WWW-Authenticate"), isNullOrBlank)
    }

    @Test
    fun `valid credentials with Auth Qop`() {
        val handler = ServerFilters.DigestAuth(realm, passwordLookup, qop = listOf(Qop.Auth)).then { Response(Status.OK) }
        val response = ClientFilters.DigestAuth(Credentials("admin", "password")).then(handler)(Request(Method.GET, "/"))

        assertThat(response.status, equalTo(Status.OK))
        assertThat(response.header("WWW-Authenticate"), isNullOrBlank)
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
        val handler = ServerFilters.DigestAuth(realm, passwordLookup, qop = emptyList()).then { Response(Status.OK) }
        val response = ClientFilters.DigestAuth(Credentials("admin", "password")).then(handler)(Request(Method.GET, "/"))

        assertThat(response.status, equalTo(Status.OK))
        assertThat(response.header("WWW-Authenticate"), isNullOrBlank)
    }
}
