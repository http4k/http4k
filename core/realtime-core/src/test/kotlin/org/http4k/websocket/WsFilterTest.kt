package org.http4k.websocket

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.http4k.core.Method.GET
import org.http4k.core.Request
import org.http4k.core.Uri
import org.http4k.routing.path
import org.http4k.routing.ws.bind
import org.junit.jupiter.api.Test

class WsFilterTest {

    private val messages = mutableListOf<String>()

    private val inner = WsFilter { next ->
        {
            messages += "inner filter in"
            next(it).also {
                messages += "inner filter out"
            }
        }
    }

    private val first = WsFilter { next ->
        { req ->
            messages += "first filter in"
            next(req.header("foo", "newHeader")).also {
                messages += "first filter out"
            }
        }
    }

    private val second = WsFilter { next ->
        {
            messages += "second filter in"
            next(it).also {
                messages += "second filter out"
            }
        }
    }

    private val ws = "/{foobar}" bind inner.then { it: Request ->
        messages += it.path("foobar")!!
        messages += it.header("foo")!!
        WsResponse { _ -> }
    }

    @Test
    fun `can manipulate value on way in and out of service`() {
        val svc = first.then(second).then(ws)
        val request = Request(GET, Uri.of("/path"))

        svc(request)

        assertThat(
            messages, equalTo(
                listOf(
                    "first filter in",
                    "second filter in",
                    "inner filter in",
                    "path",
                    "newHeader",
                    "inner filter out",
                    "second filter out",
                    "first filter out"
                )
            )
        )
    }
}
