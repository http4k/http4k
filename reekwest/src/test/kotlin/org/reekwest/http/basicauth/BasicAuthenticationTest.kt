package org.reekwest.http.basicauth

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.junit.Test
import org.reekwest.http.core.Request
import org.reekwest.http.core.Request.Companion.get
import org.reekwest.http.core.Status.Companion.OK
import org.reekwest.http.core.Status.Companion.UNAUTHORIZED
import org.reekwest.http.core.header
import org.reekwest.http.core.ok

class BasicAuthenticationTest {
    @Test
    fun fails_to_authenticate() {
        val handler = { _: Request -> ok() }.basicAuthProtected("my realm", "user", "password")
        val response = handler(get("/"))
        assertThat(response.status, equalTo(UNAUTHORIZED))
        assertThat(response.header("WWW-Authenticate"), equalTo("Basic Realm=\"my realm\""))
    }

    @Test
    fun authenticate_using_client_extension() {
        val handler = { _: Request -> ok() }.basicAuthProtected("my realm", "user", "password")
        val response = handler.basicAuth("user", "password")(get("/"))
        assertThat(response.status, equalTo(OK))
    }

    @Test
    fun fails_to_authenticate_if_credentials_do_not_match() {
        val handler = { _: Request -> ok() }.basicAuthProtected("my realm", "user", "password")
        val response = handler.basicAuth("user", "wrong")(get("/"))
        assertThat(response.status, equalTo(UNAUTHORIZED))
    }

    @Test
    fun allow_injecting_authorize_function() {
        val credential = Credentials("user", "pass")
        val handler = { _: Request -> ok() }.basicAuthProtected("my realm", { candidate -> candidate == credential })
        val response = handler.basicAuth(credential)(get("/"))
        assertThat(response.status, equalTo(OK))
    }
}
