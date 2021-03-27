package org.http4k.client

import org.apache.hc.client5.http.config.RequestConfig
import org.apache.hc.client5.http.impl.classic.HttpClients
import org.apache.hc.core5.util.Timeout
import org.http4k.core.BodyMode.Stream
import org.http4k.core.Method
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status
import org.http4k.core.then
import org.http4k.filter.ServerFilters
import org.http4k.server.ApacheServer
import org.http4k.server.SunHttp
import org.http4k.server.asServer
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test

class JavaHttpClientTest : HttpClientContract(
    ::ApacheServer, JavaHttpClient(),
    ApacheClient(
        HttpClients.custom()
            .setDefaultRequestConfig(
                RequestConfig.custom()
                    .setResponseTimeout(Timeout.ofMilliseconds(100))
                    .build()
            ).build(), responseBodyMode = Stream
    )
) {

    @Disabled("unsupported by the underlying java client")
    override fun `handles response with custom status message`() {
        super.`handles response with custom status message`()
    }

    @Test
    fun `supports gzipped content`() {
        val asServer = ServerFilters.GZip().then { Response(Status.OK).body("hello") }.asServer(SunHttp(0))
        asServer.start()
        val client = JavaHttpClient()

        val request = Request(Method.GET, "http://localhost:${asServer.port()}").header("accept-encoding", "gzip")
        client(request)
        client(request)
        client(request)
        asServer.stop()
    }
}
