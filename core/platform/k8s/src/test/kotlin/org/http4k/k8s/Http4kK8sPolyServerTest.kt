package org.http4k.k8s

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.http4k.client.JavaHttpClient
import org.http4k.client.WebsocketClient
import org.http4k.config.Environment
import org.http4k.config.EnvironmentKey
import org.http4k.core.ContentType
import org.http4k.core.Method
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status
import org.http4k.core.Uri
import org.http4k.core.with
import org.http4k.hamkrest.hasStatus
import org.http4k.lens.accept
import org.http4k.routing.bindHttp
import org.http4k.routing.bindSse
import org.http4k.routing.bindWs
import org.http4k.routing.poly
import org.http4k.server.Helidon
import org.http4k.sse.SseResponse
import org.http4k.util.PortBasedTest
import org.http4k.websocket.WsResponse
import org.http4k.websocket.WsStatus
import org.junit.jupiter.api.AutoClose
import org.junit.jupiter.api.Test

class Http4kK8sPolyServerTest : PortBasedTest {

    private val app =
        poly(
            "http" bindHttp { _: Request -> Response.Companion(Status.I_M_A_TEAPOT) },
            "sse" bindSse { _: Request -> SseResponse.Companion { it.close() } },
            "ws" bindWs { _: Request -> WsResponse.Companion { it.close(WsStatus.NORMAL) } }
        )
    private val env = Environment.EMPTY.with(EnvironmentKey.k8s.SERVICE_PORT of 0, EnvironmentKey.k8s.HEALTH_PORT of 0)

    @AutoClose("stop")
    private val server = app.asK8sServer(::Helidon, env).also { it.start() }

    private val client = JavaHttpClient()

    @Test
    fun `http app is available on port`() {
        assertThat(
            client(Request.Companion(Method.GET, "http://localhost:${server.port()}/http")),
            hasStatus(Status.I_M_A_TEAPOT)
        )
    }

    @Test
    fun `sse endpoint is reachable`() {
        assertThat(
            client(
                Request.Companion(Method.GET, "http://localhost:${server.port()}/sse")
                    .accept(ContentType.TEXT_EVENT_STREAM)
            ).status,
            equalTo(Status.OK)
        )
    }

    @Test
    fun `ws endpoint is reachable`() {
        val ws = WebsocketClient().blocking(Uri.of("ws://localhost:${server.port()}/ws"))
        assertThat(ws.received().none(), equalTo(true))
    }

    @Test
    fun `health is available`() {
        assertThat(
            client(Request.Companion(Method.GET, "http://localhost:${server.healthPort()}/liveness")),
            hasStatus(Status.OK)
        )
        assertThat(
            client(Request.Companion(Method.GET, "http://localhost:${server.healthPort()}/readiness")),
            hasStatus(Status.OK)
        )
    }
}
