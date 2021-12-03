package org.http4k.sse

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.http4k.core.Method.GET
import org.http4k.core.Request
import org.http4k.core.Uri
import org.http4k.routing.bind
import org.http4k.routing.path
import org.http4k.routing.sse
import org.junit.jupiter.api.Test

class SseFilterTest {

    private val messages = mutableListOf<String>()

    private val inner = SseFilter { next ->
        {
            messages += "inner filter in"
            next(it)
            messages += "inner filter out"
        }
    }

    private val first = SseFilter { next ->
        {
            messages += "first filter in"
            next(object : Sse by it {
                override val connectRequest: Request = it.connectRequest.header("foo", "newHeader")
            })
            messages += "first filter out"
        }
    }

    private val second = SseFilter { next ->
        {
            messages += "second filter in"
            next(it)
            messages += "second filter out"
        }
    }

    private val sse = sse("/{foobar}" bind inner.then {
        messages += it.connectRequest.path("foobar")!!
        messages += it.connectRequest.header("foo")!!
    })

    @Test
    fun `can manipulate value on way in and out of service`() {
        val svc = first.then(second).then(sse)
        val request = Request(GET, Uri.of("/path"))

        svc(request)(object : Sse {
            override val connectRequest: Request = request

            override fun send(message: SseMessage) {
            }

            override fun close() {
            }

            override fun onClose(fn: () -> Unit) {
            }
        }
        )

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
