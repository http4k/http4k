package org.http4k.server

import com.natpryce.hamkrest.Matcher
import com.natpryce.hamkrest.equalTo
import org.http4k.client.ApacheClient
import org.http4k.client.JavaHttpClient
import org.http4k.core.Method.DELETE
import org.http4k.core.Method.GET
import org.http4k.core.Method.POST
import org.http4k.core.Method.PUT
import org.http4k.sse.SseServerContract
import org.http4k.websocket.WebsocketServerContract

class UndertowTest : ServerContract({ Undertow(it) }, ApacheClient()) {
    override fun requestScheme(): Matcher<String?> = equalTo("http")
}

class UndertowWebsocketTest : WebsocketServerContract(::Undertow, JavaHttpClient())

class UndertowSseTest : SseServerContract(::Undertow, JavaHttpClient()) {
    override val supportedMethods = setOf(GET, POST, DELETE, PUT)
}
