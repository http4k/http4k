package com.gourame.http.servlet

import com.gourame.http.apache.ApacheHttpClient
import com.gourame.http.core.Entity
import com.gourame.http.core.Method
import com.gourame.http.core.Request
import com.gourame.http.core.Response
import com.gourame.http.core.Status
import com.gourame.http.core.Uri
import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.junit.Test

class HttpServletTest {

    @Test
    fun can_use_as_servlet() {
        val server = JettyServer({ request -> Response(Status.OK, mapOf(), Entity("Hello World")) }, 8000).start()
        val client = ApacheHttpClient()
        assertThat(client(Request(Method.GET, Uri("http://localhost:8000/"))).entity, equalTo(Entity("Hello World")))
        server.stop()
    }
}

