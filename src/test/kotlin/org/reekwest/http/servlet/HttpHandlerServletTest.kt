package org.reekwest.http.servlet

import org.reekwest.http.apache.ApacheHttpClient
import org.reekwest.http.core.Entity
import org.reekwest.http.core.Method.GET
import org.reekwest.http.core.Request
import org.reekwest.http.core.Request.Companion.get
import org.reekwest.http.core.Response
import org.reekwest.http.core.Status.Companion.OK
import org.reekwest.http.jetty.startJettyServer
import org.reekwest.http.routing.by
import org.reekwest.http.routing.routes
import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.junit.Test

class HttpHandlerServletTest {

    @Test
    fun can_use_as_servlet() {
        val server = routes(
            GET to "/" by { _: Request -> Response(OK, entity = Entity("Hello World")) }
        ).startJettyServer()

        val client = ApacheHttpClient()
        val response = client(get("http://localhost:8000/"))

        assertThat(response.entity, equalTo(Entity("Hello World")))
        server.stop()
    }
}

