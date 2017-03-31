package org.reekwest.http.servlet

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.junit.Test
import org.reekwest.http.apache.ApacheHttpClient
import org.reekwest.http.core.Entity
import org.reekwest.http.core.Method.GET
import org.reekwest.http.core.Request
import org.reekwest.http.core.Request.Companion.get
import org.reekwest.http.core.Response
import org.reekwest.http.core.Status.Companion.OK
import org.reekwest.http.core.headerValues
import org.reekwest.http.jetty.startJettyServer
import org.reekwest.http.routing.by
import org.reekwest.http.routing.routes

class HttpHandlerServletTest {
    init {
        routes(
            GET to "/" by { _: Request -> Response(OK, entity = Entity("Hello World")) },
            GET to "/request-headers" by { request: Request -> Response(OK, entity = Entity(request.headerValues("foo").joinToString(", "))) }
        ).startJettyServer()
    }

    private val client = ApacheHttpClient()

    @Test
    fun can_use_as_servlet() {
        val client = client
        val response = client(get("http://localhost:8000/"))

        assertThat(response.entity, equalTo(Entity("Hello World")))
    }

    @Test
    fun can_handle_multiple_request_headers() {
        val client = client
        val response = client(get("http://localhost:8000/request-headers", listOf("foo" to "one", "foo" to "two", "foo" to "three")))

        assertThat(response.entity, equalTo(Entity("one, two, three")))
    }

}

