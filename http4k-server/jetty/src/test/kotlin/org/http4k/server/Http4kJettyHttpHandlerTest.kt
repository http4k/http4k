package org.http4k.server

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import com.natpryce.hamkrest.hasElement
import dev.forkhandles.mock4k.MockMode
import dev.forkhandles.mock4k.mock
import org.eclipse.jetty.http.HttpField
import org.eclipse.jetty.http.HttpFields
import org.eclipse.jetty.http.HttpURI
import org.eclipse.jetty.server.ConnectionMetaData
import org.eclipse.jetty.util.Callback
import org.http4k.core.HttpHandler
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status
import org.junit.jupiter.api.Test
import java.net.InetSocketAddress
import java.nio.ByteBuffer
import org.eclipse.jetty.server.Request as JettyRequest
import org.eclipse.jetty.server.Response as JettyResponse

class Http4kJettyHttpHandlerTest {

    @Test
    fun `header with quotes keep their quotes`() {
        val request = FakeRequest("if-none-match" to "\"900150983cd24fb0d6963f7d28e17f72\"")
        val response = FakeResponse()
        val callback = object : Callback by mock(MockMode.Relaxed) {}

        val httpHandler = FakeHttpHandler()

        Http4kJettyHttpHandler(httpHandler).handle(request, response, callback)

        assertThat("http handler was called", httpHandler.wasCalled, equalTo(true))
        assertThat(httpHandler.capturedHeaders, hasElement("if-none-match" to "\"900150983cd24fb0d6963f7d28e17f72\""))

    }

    private class FakeHttpHandler : HttpHandler {
        var wasCalled = false
        var capturedHeaders = emptyList<Pair<String, String?>>()

        override fun invoke(request: Request): Response {
            wasCalled = true
            capturedHeaders = request.headers
            return Response(Status.OK)
        }
    }

    private class FakeRequest(private vararg val headers: Pair<String, String>) : JettyRequest by mock() {
        override fun getHeaders(): HttpFields =
            HttpFields.from(*headers.map { (k, v) -> HttpField(k, v) }.toTypedArray())

        override fun getMethod(): String = "GET"

        override fun getHttpURI() = HttpURI.from("http://localhost")

        override fun getConnectionMetaData(): ConnectionMetaData = object : ConnectionMetaData by mock() {
            override fun getRemoteSocketAddress() = InetSocketAddress(80)
        }
    }

    private class FakeResponse: JettyResponse by mock(MockMode.Relaxed) {
        override fun write(last: Boolean, byteBuffer: ByteBuffer?, callback: Callback) {
            callback.succeeded()
        }
    }
}
