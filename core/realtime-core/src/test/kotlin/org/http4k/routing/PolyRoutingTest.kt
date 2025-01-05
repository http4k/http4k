package org.http4k.routing

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.http4k.core.HttpHandler
import org.http4k.core.Response
import org.http4k.core.Status.Companion.OK
import org.http4k.server.PolyHandler
import org.http4k.util.PortBasedTest
import org.junit.jupiter.api.Test

class PolyRoutingTest : PortBasedTest {

    @Test
    fun `can use DSL to construct polyhandler`() {
        val websockets = websockets {

        }
        val serverSentEvents = sse {}
        val httpHandler: HttpHandler = { Response(OK) }

        val poly = routes {
            http = httpHandler
            ws = websockets
            sse = serverSentEvents
        }

        assertThat(poly, equalTo(PolyHandler(httpHandler, websockets, serverSentEvents)))
    }
}
