package org.http4k.server

import com.natpryce.hamkrest.Matcher
import com.natpryce.hamkrest.equalTo
import org.http4k.client.ApacheClient
import org.http4k.client.JavaHttpClient
import org.http4k.sse.SseServerContract
import org.http4k.websocket.WebsocketServerContract

class UndertowTest : ServerContract({ Undertow(it) }, ApacheClient()) {
    override fun requestScheme(): Matcher<String?> = equalTo("http")
}

class UndertowStopTest : ServerStopContract(
    { stopMode -> Undertow(0, false, stopMode) },
    ApacheClient(),
    {
        enableImmediateStop()
        enableGracefulStop()
    })

class UndertowWebsocketTest : WebsocketServerContract(::Undertow, JavaHttpClient())

class UndertowSseTest : SseServerContract(::Undertow, JavaHttpClient())
