package org.http4k.connect.mcp

import org.http4k.filter.debug
import org.http4k.routing.sse
import org.http4k.server.Helidon
import org.http4k.server.asServer
import org.http4k.sse.SseMessage.Event

fun main() {
    sse {
        it.send(Event("endpoint", "/message?sessionId=f9f60485-697e-4564-8c23-5a86dd174a92\t\n"))
        println(it.connectRequest.bodyString())
    }.debug(debugStream = true).asServer(Helidon(4001)).start()
}


