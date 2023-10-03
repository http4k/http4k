package org.http4k.serverless

import com.microsoft.azure.functions.HttpMethod
import com.microsoft.azure.functions.HttpRequestMessage
import com.microsoft.azure.functions.HttpResponseMessage
import com.microsoft.azure.functions.HttpResponseMessage.Builder
import com.microsoft.azure.functions.HttpStatus
import com.microsoft.azure.functions.HttpStatusType
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status
import org.http4k.core.queries
import java.net.URI
import java.util.Optional

class FakeAzureRequest(val request: Request) : HttpRequestMessage<Optional<String>> {
    override fun getUri() = URI.create(request.uri.toString())

    override fun getHttpMethod() = HttpMethod.value(request.method.name)

    override fun getHeaders() = request.headers.associate { it.first to (it.second ?: "") }.toMutableMap()

    override fun getQueryParameters() = mutableMapOf(
        *request.uri.queries().map { it.first to (it.second ?: "") }.toTypedArray()
    )

    override fun getBody(): Optional<String> = Optional.of(request.bodyString())

    override fun createResponseBuilder(status: HttpStatus) = Http4kBuilder(Status(status.value(), ""))

    override fun createResponseBuilder(status: HttpStatusType): Builder = Http4kBuilder(Status(status.value(), ""))
}

class Http4kBuilder(status: Status) : Builder {
    private var response = Response(status)

    override fun status(status: HttpStatusType) = apply {
        response = response.status(Status(status.value(), ""))
    }

    override fun header(key: String, value: String) = apply {
        response = response.header(key, value)
    }

    override fun body(body: Any) = apply {
        response = response.body(body.toString())
    }

    override fun build(): HttpResponseMessage = Http4kResponse(response)
}

data class Http4kResponse(val response: Response) : HttpResponseMessage {
    override fun getStatus() = HttpStatus.valueOf(response.status.code)

    override fun getHeader(key: String) = response.header(key)

    override fun getBody() = response.bodyString()
}
