package com.gourame.http.servlet

import com.gourame.http.apache.ApacheHttpClient
import com.gourame.http.core.Entity
import com.gourame.http.core.Request
import com.gourame.http.core.Response
import com.gourame.http.core.Status.Companion.OK
import com.gourame.http.jetty.asJettyServer
import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.junit.Test

class HttpHandlerServletTest {

    @Test
    fun can_use_as_servlet() {
        val server = { request:Request -> Response(OK, mapOf(), Entity("Hello World")) }.asJettyServer().start()
        val client = ApacheHttpClient()
        assertThat(client(Request.get("http://localhost:8000/")).entity, equalTo(Entity("Hello World")))
        server.stop()
    }
}

