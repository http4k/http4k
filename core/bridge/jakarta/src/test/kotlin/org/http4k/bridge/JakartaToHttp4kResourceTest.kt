package org.http4k.bridge

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import dev.forkhandles.mock4k.mock
import jakarta.ws.rs.core.HttpHeaders
import jakarta.ws.rs.core.MultivaluedHashMap
import jakarta.ws.rs.core.MultivaluedMap
import jakarta.ws.rs.core.UriInfo
import org.http4k.core.Response
import org.http4k.core.Status.Companion.OK
import org.junit.jupiter.api.Test
import java.io.InputStream
import java.net.URI
import jakarta.ws.rs.core.Request as JRequest
import jakarta.ws.rs.core.Response as JResponse

class JakartaToHttp4kResourceTest {

    class MyResource : JakartaToHttp4kResource() {
        init {
            http = { Response(OK).body(it.body).headers(it.headers) }
        }
    }

    @Test
    fun `bridges between jakarta and http4k`() {

        MyResource().get(
            object : JRequest by mock() {
                override fun getMethod() = "GET"
            },
            object : UriInfo by mock() {
                override fun getRequestUri() = URI("http://localhost?a=b")
            },
            object : HttpHeaders by mock() {
                override fun getRequestHeaders(): MultivaluedMap<String, String> {
                    return MultivaluedHashMap()
                }
            },
            "hello world".byteInputStream()
        ).let {
            assertThat(it.status, equalTo(JResponse.Status.OK.statusCode))
            assertThat(it.headers, equalTo(emptyMap()))
            assertThat((it.entity as InputStream).reader().readText(), equalTo("hello world"))
        }

    }
}
