package org.http4k.server

import org.apache.hc.client5.http.classic.methods.HttpUriRequestBase
import org.apache.hc.client5.http.impl.classic.HttpClients
import org.http4k.client.ApacheClient
import org.http4k.core.BodyMode
import org.http4k.core.BodyMode.Memory
import org.http4k.core.HttpHandler
import org.http4k.core.Status
import java.net.URI.create

object ClientForServerTesting {
    fun makeRequestWithInvalidMethod(baseUrl: String): Status =
        HttpClients.createDefault().use { client ->
            client.execute(HttpUriRequestBase("UNKNWON", create(baseUrl))) { it.code }
                .let(Status::fromCode)!!
        }

    operator fun invoke(bodyMode: BodyMode = Memory): HttpHandler =
        ApacheClient(requestBodyMode = bodyMode, responseBodyMode = bodyMode)
}
