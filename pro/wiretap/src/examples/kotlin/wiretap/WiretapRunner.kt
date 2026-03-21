/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package wiretap

import org.http4k.client.JavaHttpClient
import org.http4k.core.Method.GET
import org.http4k.core.Request
import org.http4k.server.Jetty
import org.http4k.server.asServer
import org.http4k.server.uri

fun main() {
//    val wiretap = HttpApp()
//    val wiretap = HttpAppWithOtelTracing()
//    val wiretap = LocalHttpAppWithOtelTracing()
//    val wiretap = McpApp()
//    val wiretap = McpServer()
//    val wiretap = McpServerWithOtel()
    val wiretap = LocalMcpServerWithOtel()
//    val wiretap = OpenApiApp()
//    val wiretap = ExternalMcpServer()
//    val wiretap = ExternalMcpApp()
//    val wiretap = ExternalWebsite()

//    println(
//        HttpNonStreamingMcpClient(Uri.of("https://demo.http4k.org/mcp-sdk/mcp")).apply { start() }
//            .tools().list()
//    )

    val server = wiretap.asServer(Jetty(21000)).start()

    println("started ${server.uri().path("_wiretap")}")

    (0..5).forEach {
        JavaHttpClient()(Request(GET, server.uri()))
    }

}


