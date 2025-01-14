package org.http4k.connect.mcp

import org.http4k.connect.mcp.ProtocolVersion.Companion.LATEST_VERSION
import org.http4k.routing.mcp
import org.http4k.server.Helidon
import org.http4k.server.asServer

fun main() {
    val sse = mcp(
        Implementation("mcp-kotlin test server", Version.of("0.1.0")),
        LATEST_VERSION,
    )

    sse.asServer(Helidon(3001)).start()
}


