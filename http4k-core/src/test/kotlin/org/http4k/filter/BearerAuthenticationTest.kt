package org.http4k.filter

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.http4k.core.Credentials
import org.http4k.core.Method.GET
import org.http4k.core.Request
import org.http4k.core.RequestContexts
import org.http4k.core.Response
import org.http4k.core.Status.Companion.OK
import org.http4k.core.Status.Companion.UNAUTHORIZED
import org.http4k.core.then
import org.http4k.filter.ServerFilters.BearerAuth
import org.http4k.filter.ServerFilters.InitialiseRequestContext
import org.http4k.lens.RequestContextKey
import org.http4k.security.CredentialsProvider
import org.junit.jupiter.api.Test

class BearerAuthenticationTest {

    @Test
    fun wrong_token_type() {
        val handler = ServerFilters.BearerAuth("Basic dXNlcjpwYXNzd29yZA==").then { Response(OK) }
        val response = ClientFilters.BasicAuth("user", "password")
            .then(handler)(Request(GET, "/"))
        assertThat(response.status, equalTo(UNAUTHORIZED))
    }

    @Test
    fun fails_to_authenticate() {
        val handler = ServerFilters.BearerAuth("token").then { Response(OK) }
        val response = handler(Request(GET, "/"))
        assertThat(response.status, equalTo(UNAUTHORIZED))
    }

    @Test
    fun authenticate_using_client_extension() {
        val handler = ServerFilters.BearerAuth("token").then { Response(OK) }
        val response = ClientFilters.BearerAuth(CredentialsProvider { "token" }).then(handler)(Request(GET, "/"))
        assertThat(response.status, equalTo(OK))
    }

    @Test
    fun fails_to_authentic_with_non_bearer_token() {
        val handler = ServerFilters.BearerAuth("Basic YmFkZ2VyOm1vbmtleQ==").then { Response(OK) }
        val response = ClientFilters.BasicAuth("badger", "monkey").then(handler)(Request(GET, "/"))
        assertThat(response.status, equalTo(UNAUTHORIZED))
    }

    @Test
    fun fails_to_authenticate_if_credentials_do_not_match() {
        val handler = ServerFilters.BearerAuth("token").then { Response(OK) }
        val response = ClientFilters.BearerAuth(CredentialsProvider { "not token" }).then(handler)(Request(GET, "/"))
        assertThat(response.status, equalTo(UNAUTHORIZED))
    }

    @Test
    fun populates_request_context_for_later_retrieval() {
        val contexts = RequestContexts()
        val key = RequestContextKey.required<Credentials>(contexts)

        val handler =
            InitialiseRequestContext(contexts)
                .then(BearerAuth(key) { Credentials(it, it) })
                .then { req -> Response(OK).body(key(req).toString()) }

        val response = ClientFilters.BearerAuth("token").then(handler)(Request(GET, "/"))

        assertThat(response.bodyString(), equalTo("Credentials(user=token, password=token)"))
    }
}
