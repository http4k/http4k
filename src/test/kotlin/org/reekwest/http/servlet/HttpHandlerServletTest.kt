package org.reekwest.http.servlet

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.reekwest.http.apache.ApacheHttpClient
import org.reekwest.http.core.Entity
import org.reekwest.http.core.Method.GET
import org.reekwest.http.core.Request
import org.reekwest.http.core.Response
import org.reekwest.http.core.Status.Companion.OK
import org.reekwest.http.core.StringEntity
import org.reekwest.http.core.extract
import org.reekwest.http.core.get
import org.reekwest.http.core.headerValues
import org.reekwest.http.core.toEntity
import org.reekwest.http.jetty.JettyServer
import org.reekwest.http.jetty.startJettyServer
import org.reekwest.http.routing.by
import org.reekwest.http.routing.routes

class HttpHandlerServletTest {
    var server: JettyServer? = null
    private val client = ApacheHttpClient()

    @Before
    fun before() {
        server = routes(
            GET to "/" by { _: Request -> Response(OK, entity = "Hello World".toEntity()) },
            GET to "/request-headers" by { request: Request -> Response(OK, entity = Entity(request.headerValues("foo").joinToString(", "))) }
        ).startJettyServer(block = false)
    }

    @Test
    fun can_use_as_servlet() {
        val client = client
        val response = client(get("http://localhost:8000/"))

        assertThat(response.extract(StringEntity), equalTo("Hello World"))
    }

    @Test
    fun can_handle_multiple_request_headers() {
        val client = client
        val response = client(get("http://localhost:8000/request-headers", listOf("foo" to "one", "foo" to "two", "foo" to "three")))

        assertThat(response.extract(StringEntity), equalTo("one, two, three"))
    }

    @After
    fun after() {
        server?.stop()
    }

}

