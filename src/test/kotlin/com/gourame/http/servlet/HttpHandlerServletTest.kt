package com.gourame.http.servlet

import com.gourame.http.apache.ApacheHttpClient
import com.gourame.http.core.Entity
import com.gourame.http.core.Request
import com.gourame.http.core.Request.Companion.get
import com.gourame.http.core.Response
import com.gourame.http.core.Status.Companion.OK
import com.gourame.http.jetty.startJettyServer
import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.junit.Test

class HttpHandlerServletTest {

    @Test
    fun can_use_as_servlet() {
        val server = { _: Request -> Response(OK, entity = Entity("Hello World")) }.startJettyServer()
        val client = ApacheHttpClient()
        assertThat(client(get("http://localhost:8000/")).entity, equalTo(Entity("Hello World")))
        server.stop()
    }
}

