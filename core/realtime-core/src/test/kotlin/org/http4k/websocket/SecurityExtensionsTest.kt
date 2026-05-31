package org.http4k.websocket

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.http4k.core.Credentials
import org.http4k.core.Filter
import org.http4k.core.Method.GET
import org.http4k.core.Request
import org.http4k.lens.basicAuthentication
import org.http4k.security.BasicAuthSecurity
import org.http4k.security.Security
import org.junit.jupiter.api.Test
import java.util.concurrent.atomic.AtomicReference

class SecurityExtensionsTest {

    @Test
    fun `passed security forwards to next`() {
        val seen = AtomicReference<Request>()
        val app = WsFilter(BasicAuthSecurity("") { true }).then { req -> seen.set(req); WsResponse {} }
        app(Request(GET, "/").basicAuthentication(Credentials("", "")))
        assertThat(seen.get().uri.path, equalTo("/"))
    }

    @Test
    fun `failed security does not invoke next and refuses`() {
        val seen = AtomicReference<Request>()
        val captured = AtomicReference<WsStatus>()
        val app = WsFilter(BasicAuthSecurity("") { true }).then { req -> seen.set(req); WsResponse {} }

        val response = app(Request(GET, "/"))
        response.consumer(FakeWebsocket(onClose = { captured.set(it) }))

        assertThat(seen.get(), equalTo(null))
        assertThat(captured.get(), equalTo(WsStatus.REFUSE))
    }

    @Test
    fun `forwards security-modified request to next`() {
        val tagging = object : Security {
            override val filter = Filter { next -> { next(it.header("X-Principal", "alice")) } }
        }
        val seen = AtomicReference<Request>()
        val app = WsFilter(tagging).then { req -> seen.set(req); WsResponse {} }

        app(Request(GET, "/"))

        assertThat(seen.get().header("X-Principal"), equalTo("alice"))
    }
}

private class FakeWebsocket(private val onClose: (WsStatus) -> Unit) : Websocket {
    override fun send(message: WsMessage) {}
    override fun close(status: WsStatus) { onClose(status) }
    override fun onError(fn: (Throwable) -> Unit) {}
    override fun onClose(fn: (WsStatus) -> Unit) {}
    override fun onMessage(fn: (WsMessage) -> Unit) {}
}
