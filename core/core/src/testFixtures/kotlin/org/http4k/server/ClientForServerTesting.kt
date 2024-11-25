package org.http4k.server

import org.apache.hc.client5.http.classic.methods.HttpUriRequestBase
import org.apache.hc.client5.http.impl.classic.HttpClients.createDefault
import org.apache.hc.core5.http.ClassicHttpResponse
import org.http4k.client.ApacheClient
import org.http4k.core.BodyMode
import org.http4k.core.BodyMode.Memory
import org.http4k.core.HttpHandler
import org.http4k.core.Status
import java.net.URI.create

object ClientForServerTesting {
    fun makeRequestWithInvalidMethod(baseUrl: String): Status =
        createDefault().use { client ->
            client.execute(HttpUriRequestBase("FOOBAR", create(baseUrl)), ClassicHttpResponse::getCode)
                ?.let(Status::fromCode)!!
        }

    operator fun invoke(requestBodyMode: BodyMode = Memory, responseBodyMode: BodyMode = Memory): HttpHandler =
        ApacheClient(requestBodyMode = requestBodyMode, responseBodyMode = responseBodyMode)
}
