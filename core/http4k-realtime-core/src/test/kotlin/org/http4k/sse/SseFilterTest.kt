package org.http4k.sse

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.http4k.core.Method.GET
import org.http4k.core.Request
import org.http4k.core.Uri
import org.http4k.routing.path
import org.http4k.routing.sse.bind
import org.junit.jupiter.api.Test

class SseFilterTest {

    private val messages = mutableListOf<String>()

    private val inner = SseFilter { next ->
        {
            messages += "inner filter in"
            next(it).also {
                messages += "inner filter out"
            }
        }
    }

    private val first = SseFilter { next ->
        {
            messages += "first filter in"
            next(it.header("foo", "newHeader")).also {
                messages += "first filter out"
            }
        }
    }

    private val second = SseFilter { next ->
        {
            messages += "second filter in"
            next(it).also {
                messages += "second filter out"
            }
        }
    }

    private val sse = "/{foobar}" bind inner.then { it: Request ->
        messages += it.path("foobar")!!
        messages += it.header("foo")!!
        SseResponse { _ -> }
    }

    @Test
    fun `can manipulate value on way in and out of service`() {
        val svc = first.then(second).then(sse)
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
