package org.http4k.websocket

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.http4k.core.Method
import org.http4k.core.Request
import org.http4k.core.Uri
import org.http4k.routing.bind
import org.http4k.routing.path
import org.http4k.routing.websockets
import org.junit.jupiter.api.Test

class WsFilterTest {

    private val messages = mutableListOf<String>()

    private val ws = websockets("/{foobar}" bind { it: Websocket ->
        messages += it.upgradeRequest.path("foobar")!!
        messages += it.upgradeRequest.header("foo")!!
    })

    private val addRequestHeader = WsFilter { next ->
        {
            messages += "first filter in"
            next(object : Websocket by it {
                override val upgradeRequest: Request = it.upgradeRequest.header("foo", "newHeader")
            })
            messages += "first filter out"
        }
    }

    private val printResult = WsFilter { next ->
        {
            messages += "second filter in"
            next(it)
            messages += "second filter out"
        }
    }

    @Test
    fun `can manipulate value on way in and out of service`() {
        val svc = addRequestHeader.then(printResult).then(ws)
        val request = Request(Method.GET, Uri.of("/path"))

        svc(request).invoke(object : Websocket {
            override val upgradeRequest: Request = request

            override fun send(message: WsMessage) {
            }

            override fun close(status: WsStatus) {
            }

            override fun onError(fn: (Throwable) -> Unit) {
            }

            override fun onClose(fn: (WsStatus) -> Unit) {
            }

            override fun onMessage(fn: (WsMessage) -> Unit) {
            }
        }
        )

        assertThat(
            messages, equalTo(
                listOf(
                    "first filter in",
                    "second filter in",
                    "path",
                    "newHeader",
                    "second filter out",
                    "first filter out"
                )
            )
        )
    }
}
