package org.http4k.connect.mcp

import org.http4k.client.JavaSseClient
import org.http4k.core.ContentType
import org.http4k.core.Method
import org.http4k.core.Request
import org.http4k.lens.accept
import org.http4k.lens.contentType
import org.http4k.sse.SseMessage
import java.util.UUID

fun main() {
    val port = 3001
    JavaSseClient()(
        Request(Method.GET, "http://localhost:$port/sse")
            .accept(ContentType.APPLICATION_JSON)
    ).use {
        it.received()
            .toList()
            .filterIsInstance<SseMessage.Event>()
            .filter { it.event == "endpoint" }
            .forEach {
                println(it)
                if (it is SseMessage.Event) {
                    JavaSseClient()(
                        Request(Method.POST, "http://localhost:$port${it.data}")
                            .contentType(ContentType.APPLICATION_JSON.withNoDirectives())
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


