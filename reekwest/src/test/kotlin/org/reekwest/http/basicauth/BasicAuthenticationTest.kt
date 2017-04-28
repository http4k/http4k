package org.reekwest.http.basicauth

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.junit.Test
import org.reekwest.http.core.Request
import org.reekwest.http.core.Request.Companion.get
import org.reekwest.http.core.Response.Companion.ok
import org.reekwest.http.core.Status.Companion.OK
import org.reekwest.http.core.Status.Companion.UNAUTHORIZED
import org.reekwest.http.core.then

class BasicAuthenticationTest {
    @Test
    fun fails_to_authenticate() {
        val handler = BasicAuthServer("my realm", "user", "password").then { _: Request -> ok() }
        val response = handler(get("/"))
        assertThat(response.status, equalTo(UNAUTHORIZED))
        assertThat(response.header("WWW-Authenticate"), equalTo("Basic Realm=\"my realm\""))
    }

    @Test
    fun authenticate_using_client_extension() {
        val handler = BasicAuthServer("my realm", "user", "password").then { _: Request -> ok() }
        val response = BasicAuthClient("user", "password").then(handler)(get("/"))
        assertThat(response.status, equalTo(OK))
    }

    @Test
    fun fails_to_authenticate_if_credentials_do_not_match() {
        val handler = BasicAuthServer("my realm", "user", "password").then { _: Request -> ok() }
        val response = BasicAuthClient("user", "wrong").then(handler)(get("/"))
        assertThat(response.status, equalTo(UNAUTHORIZED))
    }

    @Test
    fun allow_injecting_authorize_function() {
        val handler = BasicAuthServer("my realm", { it.user == "user" && it.password == "password" }).then { _: Request -> ok() }
        val response = BasicAuthClient("user", "password").then(handler)(get("/"))
        assertThat(response.status, equalTo(OK))
    }

    @Test
    fun allow_injecting_credential_provider() {
        val handler = BasicAuthServer("my realm", "user", "password").then { _: Request -> ok() }
        val response = BasicAuthClient({ Credentials("user", "password") }).then(handler)(get("/"))
        assertThat(response.status, equalTo(OK))
    }
}
