package org.http4k.core

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.http4k.core.Status.Companion.I_M_A_TEAPOT
import org.http4k.core.Status.Companion.NOT_IMPLEMENTED
import org.http4k.core.Status.Companion.OK
import org.http4k.hamkrest.hasStatus
import org.http4k.routing.bindHttp
import org.http4k.routing.bindSse
import org.http4k.routing.bindWs
import org.http4k.routing.poly
import org.http4k.sse.SseFilter
import org.http4k.sse.SseResponse
import org.http4k.websocket.WsFilter
import org.http4k.websocket.WsResponse
import org.junit.jupiter.api.Test

class PolyHandlerTest {

    @Test
    fun `can add filter to polyhandler`() = runBlocking {
        val p = poly(
            "http" bindHttp { req: Request -> Response(OK) },
            "sse" bindSse { req: Request -> SseResponse {} },
            "ws" bindWs { req: Request -> WsResponse {} }
        )

        val decorated = Filter { { Response(NOT_IMPLEMENTED) } }
            .then(SseFilter { { SseResponse(I_M_A_TEAPOT) {} } }
                .then(
                    WsFilter { { WsResponse("SUB") {} } }.then(p)
                )
            )

        assertThat(decorated.http!!(Request(Method.GET, "")), hasStatus(NOT_IMPLEMENTED))
        assertThat(decorated.ws!!(Request(Method.GET, "")).subprotocol, equalTo("SUB"))
        assertThat(decorated.sse!!(Request(Method.GET, "")).status, equalTo(I_M_A_TEAPOT))
    }
}
