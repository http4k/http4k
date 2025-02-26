package org.http4k.mcp

import dev.forkhandles.bunting.Bunting
import dev.forkhandles.bunting.InMemoryConfig
import dev.forkhandles.bunting.int
import org.http4k.core.Uri

class McpOptions(args: Array<String>) :
    Bunting(
        args, "A proxy to talk to an SSE-based MCP server", "mcp-desktop",
        config = InMemoryConfig().apply { set("foo.bar", "configured value") }
    ) {
    val debug by switch("Write messages to the debug log")
    val url by option("Base URL of the SSE MCP server to connect to: eg. http://localhost:3001/sse")
        .map(Uri::of).required()
    val apiKey by option("API key to use to communicate with the server").secret()
    val apiKeyHeader by option("API key header name to use to communicate with the server").defaultsTo("X-Api-key")
    val bearerToken by option("Bearer token to use to communicate with the server").secret()
    val basicAuth by option("Basic Auth credentials to use to communicate with the server in the format: <user>:<password>").secret()
    val version by option().int().defaultsTo(0)
}
