package org.http4k.client

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import io.helidon.webclient.api.WebClient
import org.http4k.client.HelidonClient.makeHelidonRequest
import org.http4k.core.Method.GET
import org.http4k.core.Request
import org.http4k.core.Status.Companion.OK
import org.http4k.server.ApacheServer
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import java.time.Duration

class HelidonClientTest : HttpClientContract(
    ::ApacheServer, HelidonClient(),
    HelidonClient(WebClient.builder().readTimeout(Duration.ofMillis(100)).build())
),
    HttpClientWithMemoryModeContract {

    @Disabled
    override fun `fails with no protocol`() {
    }

    @Test
    fun `query parameters are preserved in requests made by the Helidon client`() {
        val helidonQuery = WebClient.create()
            .makeHelidonRequest(Request(GET, "http://localhost?p1=foo&p2=123&p1=bar"))
            .resolvedUri()
            .writeableQuery()
            .rawValue()

        assertThat(helidonQuery, equalTo("p1=foo&p1=bar&p2=123"))
    }

    @Test
    fun `helidon doesn't add another host header`() {
        val response = client(Request(GET, "http://localhost:$port/headers").header("Host", "localhost:$port"))
        assertThat(response.status, equalTo(OK))
        assertThat(response.bodyString(), equalTo("Host,User-Agent,Connection,Content-Length"))
    }
}
