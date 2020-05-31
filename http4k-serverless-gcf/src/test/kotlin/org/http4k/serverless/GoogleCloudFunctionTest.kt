package org.http4k.serverless

import com.google.cloud.functions.HttpRequest
import com.google.cloud.functions.HttpResponse
import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status.Companion.OK
import org.http4k.serverless.gcf.GoogleCloudFunction
import org.junit.jupiter.api.Test
import java.io.BufferedReader
import java.io.ByteArrayOutputStream
import java.io.StringReader

class GoogleCloudFunctionTest {

    class MockHttpRequest : HttpRequest {
        override fun getReader() = BufferedReader(StringReader("gcf test"))
        override fun getMethod() = "GET"
        override fun getHeaders() = mapOf<String, MutableList<String>>()
        override fun getUri() = "/"
        override fun getCharacterEncoding() = TODO()
        override fun getQuery() = TODO()
        override fun getContentLength() = TODO()
        override fun getContentType() = TODO()
        override fun getPath() = TODO()
        override fun getParts() = TODO()
        override fun getQueryParameters() = TODO()
        override fun getInputStream() = TODO()
    }

    class MockHttpResponse : HttpResponse {
        private val outStream = ByteArrayOutputStream()
        override fun getOutputStream() = outStream
        override fun setStatusCode(code: Int, message: String?) {}
        override fun getHeaders() = TODO()
        override fun setContentType(contentType: String?) = TODO()
        override fun appendHeader(header: String?, value: String?) = TODO()
        override fun getContentType() = TODO()
        override fun getWriter() = TODO()
        override fun setStatusCode(code: Int) = TODO()
    }

    @Test
    fun `adapter calls the handler and returns proper body`() {
        val app = { _: Request -> Response(OK).body("hello gcf") }
        val request = MockHttpRequest()
        val response = MockHttpResponse()

        GoogleCloudFunction(app).service(request, response)

        assertThat((String(response.outputStream.toByteArray())), equalTo("hello gcf"))
    }

}
