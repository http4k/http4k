package org.http4k.server

import com.natpryce.hamkrest.Matcher
import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.http4k.client.ApacheClient
import org.http4k.core.Method
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status.Companion.OK
import org.junit.jupiter.api.Test
import java.io.File

class JettyTest : ServerContract(::Jetty, ApacheClient()) {
    override fun requestScheme(): Matcher<String?> = equalTo("http")

    @Test
    fun `returns status with pre-defined standardized description`() {
        val response = client(Request(Method.GET, "$baseUrl/status-with-foobar-description"))

        assertThat(response.status.code, equalTo(201))
        assertThat(response.status.description, equalTo("Created"))
    }
}

class JettyHttp2Test {

    @Test
    fun `can configure http2`() {
        val server = { _: Request -> Response(OK) }.asServer(Jetty(0,
            http2(0,
                File("src/test/resources/keystore.jks").absolutePath,
                "password")))
        server.start().stop()
    }
}
