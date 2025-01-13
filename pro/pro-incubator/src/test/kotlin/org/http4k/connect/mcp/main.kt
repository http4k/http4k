package org.http4k.connect.mcp

import org.http4k.connect.mcp.ProtocolVersion.Companion.LATEST_VERSION
import org.http4k.filter.debug
import org.http4k.routing.mcp
import org.http4k.server.Helidon
import org.http4k.server.asServer

fun main() {

    val sse = mcp(
        Implementation("foo", Version.of("bar")),
        LATEST_VERSION,
    )

    sse.debug(debugStream = true).asServer(Helidon(3001)).start()
}


