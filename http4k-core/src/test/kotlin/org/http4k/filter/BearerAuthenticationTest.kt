package org.http4k.filter

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.http4k.core.Method.GET
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status.Companion.OK
import org.http4k.core.Status.Companion.UNAUTHORIZED
import org.http4k.core.then
import org.junit.jupiter.api.Test

class BearerAuthenticationTest {
    @Test
    fun fails_to_authenticate() {
        val handler = ServerFilters.BearerAuth("token").then { _: Request -> Response(OK) }
        val response = handler(Request(GET, "/"))
        assertThat(response.status, equalTo(UNAUTHORIZED))
    }

    @Test
    fun authenticate_using_client_extension() {
        val handler = ServerFilters.BearerAuth("token").then { _: Request -> Response(OK) }
        val response = ClientFilters.BearerAuth("token").then(handler)(Request(GET, "/"))
        assertThat(response.status, equalTo(OK))
    }

    @Test
    fun fails_to_authenticate_if_credentials_do_not_match() {
        val handler = ServerFilters.BearerAuth("token").then { _: Request -> Response(OK) }
        val response = ClientFilters.BearerAuth("not token").then(handler)(Request(GET, "/"))
        assertThat(response.status, equalTo(UNAUTHORIZED))
    }
}
