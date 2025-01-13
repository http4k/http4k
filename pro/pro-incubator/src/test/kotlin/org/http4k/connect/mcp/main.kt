package org.http4k.connect.mcp

import org.http4k.core.Method.GET
import org.http4k.core.Request
import org.http4k.core.Uri
import org.http4k.mcp.ToolBinding
import org.http4k.routing.mcp
import org.http4k.routing.sse
import org.http4k.server.Helidon
import org.http4k.server.asServer
import org.http4k.sse.SseMessage.Event
import org.http4k.sse.pipeSseTraffic

fun main(args: Array<String>) {


    val sse = mcp(Implementation("foo")
        , ToolBinding())
//    if (args.isEmpty()) {
//        println("Please provide the server URL as an argument")
//        return
//    }

    val server = sse {
        println(it.connectRequest.bodyString())
        it.send(Event("endpoint", "/message?sessionId=f9f60485-697e-4564-8c23-5a86dd174a92\t\n"))
    }.asServer(Helidon(0)).start()

    //    val serverUrl = args[0]
    val serverUrl = Uri.of("http://localhost:${server.port()}")

    pipeSseTraffic {
        Request(GET, serverUrl).query("foo", "bar")
    }
}
