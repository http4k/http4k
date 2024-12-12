package org.http4k.server

import com.natpryce.hamkrest.equalTo
import org.http4k.client.JavaHttpClient
import org.http4k.sse.DatastarServerContract
import org.http4k.sse.SseServerContract
import org.http4k.websocket.WebsocketServerContract

class UndertowTest : ServerContract(::Undertow, ClientForServerTesting()) {
    override fun requestScheme() = equalTo("http")
}

class UndertowWebsocketTest : WebsocketServerContract(::Undertow, JavaHttpClient())

class UndertowSseTest : SseServerContract(::Undertow, JavaHttpClient())

class UndertowDatastarTest : DatastarServerContract(::Undertow, JavaHttpClient())
