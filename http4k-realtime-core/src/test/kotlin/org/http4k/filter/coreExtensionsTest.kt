package org.http4k.filter

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.http4k.core.Credentials
import org.http4k.core.Method.GET
import org.http4k.core.Request
import org.http4k.core.RequestContexts
import org.http4k.core.with
import org.http4k.lens.RequestContextKey
import org.http4k.sse.SseFilter
import org.http4k.sse.SseResponse
import org.http4k.sse.then
import org.http4k.websocket.WsFilter
import org.http4k.websocket.WsResponse
import org.http4k.websocket.then
import org.junit.jupiter.api.Test
import java.util.concurrent.atomic.AtomicReference

class CoreExtensionsTest {
    private val contexts = RequestContexts()
    private val key = RequestContextKey.required<Credentials>(contexts)
    private val credentials = Credentials("123", "456")

    @Test
    fun `can initialise and populate sse request context`() {
        val found = AtomicReference<Credentials>(null)
        val handler = ServerFilters.InitialiseSseRequestContext(contexts)
            .then(SseFilter { next ->
                {
                    next(it.with(key of credentials))
                }
            })
            .then {
                found.set(key(it))
                SseResponse { _ -> }
            }

        handler(Request(GET, "/"))

        assertThat(found.get(), equalTo(credentials))
    }

    @Test
    fun `can initialise and populate ws request context`() {
        val found = AtomicReference<Credentials>(null)
        val handler = ServerFilters.InitialiseWsRequestContext(contexts)
            .then(WsFilter { next ->
                {
                    next(it.with(key of credentials))
                }
            })
            .then {
                found.set(key(it))
                WsResponse { _ -> }
            }

        handler(Request(GET, "/"))

        assertThat(found.get(), equalTo(credentials))
    }
}
