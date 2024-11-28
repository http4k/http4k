package org.http4k.server

import org.apache.http.client.methods.HttpEntityEnclosingRequestBase
import org.apache.http.impl.client.HttpClients
import org.http4k.client.Apache4Client
import org.http4k.core.BodyMode
import org.http4k.core.BodyMode.Memory
import org.http4k.core.HttpHandler
import org.http4k.core.Status
import java.net.URI
import java.net.URI.create

object ClientForServerTesting {
    fun makeRequestWithInvalidMethod(baseUrl: String): Status =
        HttpClients.createDefault().use { client ->
            client.execute(object: HttpEntityEnclosingRequestBase(){
                override fun getMethod(): String = "UNKNWON"
                override fun getURI(): URI = create(baseUrl)
            }).statusLine.statusCode.let(Status::fromCode)!!
        }

    operator fun invoke(bodyMode: BodyMode = Memory): HttpHandler =
        Apache4Client(requestBodyMode = bodyMode, responseBodyMode = bodyMode)
}
