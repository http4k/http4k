package org.http4k.filters

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.http4k.Credentials
import org.http4k.core.Request
import org.http4k.core.Request.Companion.get
import org.http4k.core.Response.Companion.ok
import org.http4k.core.Status.Companion.OK
import org.http4k.core.Status.Companion.UNAUTHORIZED
import org.http4k.core.then
import org.junit.Test

class BasicAuthenticationTest {
    @Test
    fun fails_to_authenticate() {
        val handler = ServerFilters.BasicAuth("my realm", "user", "password").then { _: Request -> ok() }
        val response = handler(get("/"))
        assertThat(response.status, equalTo(UNAUTHORIZED))
        assertThat(response.header("WWW-Authenticate"), equalTo("Basic Realm=\"my realm\""))
    }

    @Test
    fun authenticate_using_client_extension() {
        val handler = ServerFilters.BasicAuth("my realm", "user", "password").then { _: Request -> ok() }
        val response = ClientFilters.BasicAuth("user", "password").then(handler)(get("/"))
        assertThat(response.status, equalTo(OK))
    }

    @Test
    fun fails_to_authenticate_if_credentials_do_not_match() {
        val handler = ServerFilters.BasicAuth("my realm", "user", "password").then { _: Request -> ok() }
        val response = ClientFilters.BasicAuth("user", "wrong").then(handler)(get("/"))
        assertThat(response.status, equalTo(UNAUTHORIZED))
    }

    @Test
    fun allow_injecting_authorize_function() {
        val handler = ServerFilters.BasicAuth("my realm", { it.user == "user" && it.password == "password" }).then { _: Request -> ok() }
        val response = ClientFilters.BasicAuth("user", "password").then(handler)(get("/"))
        assertThat(response.status, equalTo(OK))
    }

    @Test
    fun allow_injecting_credential_provider() {
        val handler = ServerFilters.BasicAuth("my realm", "user", "password").then { _: Request -> ok() }
        val response = ClientFilters.BasicAuth({ Credentials("user", "password") }).then(handler)(get("/"))
        assertThat(response.status, equalTo(OK))
    }
}
