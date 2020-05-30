package org.http4k.serverless

import com.google.cloud.functions.HttpRequest
import com.google.cloud.functions.HttpResponse
import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status
import org.http4k.serverless.gcp.Http4kGCFAdapter
import org.junit.jupiter.api.Test
import java.io.*
import java.util.*

class Http4kGCFAdapterTest {

    class MockHttpRequest : HttpRequest {
        override fun getReader(): BufferedReader {
            val reader: Reader = StringReader("gcf test")
            return BufferedReader(reader)
        }

        override fun getMethod(): String = "GET"
        override fun getHeaders(): MutableMap<String, MutableList<String>> = mutableMapOf()
        override fun getUri(): String = "/"

        override fun getCharacterEncoding(): Optional<String> = TODO("Not yet implemented")
        override fun getQuery(): Optional<String> = TODO("Not yet implemented")
        override fun getContentLength(): Long = TODO("Not yet implemented")
        override fun getContentType(): Optional<String> = TODO("Not yet implemented")
        override fun getPath(): String = TODO("Not yet implemented")
        override fun getParts(): MutableMap<String, HttpRequest.HttpPart> = TODO("Not yet implemented")
        override fun getQueryParameters(): MutableMap<String, MutableList<String>> = TODO("Not yet implemented")
        override fun getInputStream(): InputStream = TODO("Not yet implemented")
    }

    class MockHttpResponse : HttpResponse {
        private val outStream = ByteArrayOutputStream()
        override fun getOutputStream(): OutputStream = outStream
        override fun setStatusCode(code: Int, message: String?) {}

        override fun getHeaders(): MutableMap<String, MutableList<String>> = TODO("Not yet implemented")
        override fun setContentType(contentType: String?) = TODO("Not yet implemented")
        override fun appendHeader(header: String?, value: String?) = TODO("Not yet implemented")
        override fun getContentType(): Optional<String> = TODO("Not yet implemented")
        override fun getWriter(): BufferedWriter = TODO("Not yet implemented")
        override fun setStatusCode(code: Int) = TODO("Not yet implemented")
    }

    @Test
    fun `adapter calls the handler and returns proper body`() {
        val app = { _: Request -> Response(Status.OK).body("hello gcf") }
        val request = MockHttpRequest()
        val response = MockHttpResponse()

        Http4kGCFAdapter(app).service(request, response)

        assertThat((String((response.outputStream as ByteArrayOutputStream).toByteArray())), equalTo("hello gcf"))
    }

}
