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
import org.http4k.lens.RequestContextKey
import org.junit.jupiter.api.Test

class BasicAuthenticationTest {

    @Test
    fun wrong_token_type() {
        val handler = ServerFilters.BasicAuth("my realm", "user", "password").then { Response(OK) }
        val response = handler(Request(GET, "/")
            .header("Authorization", "Bearer: foobar"))
        assertThat(response.status, equalTo(UNAUTHORIZED))
    }

    @Test
    fun invalid_base64() {
        val handler = ServerFilters.BasicAuth("my realm", "user", "password").then { Response(OK) }
        val response = handler(Request(GET, "/")
            .header("Authorization", "Basic ababa"))
        assertThat(response.status, equalTo(UNAUTHORIZED))
    }

    @Test
    fun fails_to_authenticate() {
        val handler = ServerFilters.BasicAuth("my realm", "user", "password").then { Response(OK) }
        val response = handler(Request(GET, "/"))
        assertThat(response.status, equalTo(UNAUTHORIZED))
        assertThat(response.header("WWW-Authenticate"), equalTo("Basic Realm=\"my realm\""))
    }

    @Test
    fun authenticate_using_client_extension() {
        val handler = ServerFilters.BasicAuth("my realm", "user", "password").then { Response(OK) }
        val response = ClientFilters.BasicAuth("user", "password").then(handler)(Request(GET, "/"))
        assertThat(response.status, equalTo(OK))
    }

    @Test
    fun fails_to_authenticate_with_non_basic_token() {
        val handler = ServerFilters.BasicAuth("my realm", "user", "password").then { Response(OK) }
        val response = ClientFilters.BearerAuth("token").then(handler)(Request(GET, "/"))
        assertThat(response.status, equalTo(UNAUTHORIZED))
    }

    @Test
    fun fails_to_authenticate_if_credentials_do_not_match() {
        val handler = ServerFilters.BasicAuth("my realm", "user", "password").then { Response(OK) }
        val response = ClientFilters.BasicAuth("user", "wrong").then(handler)(Request(GET, "/"))
        assertThat(response.status, equalTo(UNAUTHORIZED))
    }

    @Test
    fun allow_injecting_authorize_function() {
        val handler = ServerFilters.BasicAuth("my realm") { it.user == "user" && it.password == "password" }.then { Response(OK) }
        val response = ClientFilters.BasicAuth("user", "password").then(handler)(Request(GET, "/"))
        assertThat(response.status, equalTo(OK))
    }

    @Test
    fun allow_injecting_credential_provider() {
        val handler = ServerFilters.BasicAuth("my realm", "user", "password").then { Response(OK) }
        val response = ClientFilters.BasicAuth { Credentials("user", "password") }.then(handler)(Request(GET, "/"))
        assertThat(response.status, equalTo(OK))
    }

    @Test
    fun populates_request_context_for_later_retrieval() {
        val contexts = RequestContexts()
        val key = RequestContextKey.required<Credentials>(contexts)

        val handler =
            ServerFilters.InitialiseRequestContext(contexts)
                .then(ServerFilters.BasicAuth("my realm", key) { it })
                .then { req -> Response(OK).body(key(req).toString()) }

        val response = ClientFilters.BasicAuth("user", "password").then(handler)(Request(GET, "/"))

        assertThat(response.bodyString(), equalTo("Credentials(user=user, password=password)"))
    }
}
