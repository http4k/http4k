package org.http4k.connect.mcp

import org.http4k.client.JavaSseClient
import org.http4k.core.ContentType
import org.http4k.core.Method
import org.http4k.core.Request
import org.http4k.lens.accept
import org.http4k.sse.SseMessage
import java.util.UUID

fun main() {
    JavaSseClient()(
        Request(Method.GET, "http://localhost:4001/sse")
            .accept(ContentType.APPLICATION_JSON)
    ).use {
        it.received().forEach {
            println(it)
            if (it is SseMessage.Event) {
                JavaSseClient()(
                    Request(Method.POST, "http://localhost:4001${it.data}")
                        .body("""{"method": "initialize", "params": {"protocolVersion": "2024-11-05", "capabilities": {"sampling": {}, "roots": {"listChanged": true}}, "clientInfo": {"name": "mcp-inspector", "version": "0.0.1"}}, "jsonrpc": "2.0", "id": "${UUID.randomUUID()}"}""")
                        .also { println(it) }
                ).use {
                    it.received().forEach {
                        println(it)
                    }
                }
            }
        }
    }
}


