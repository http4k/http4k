package org.http4k.mcp

import dev.forkhandles.bunting.Bunting
import dev.forkhandles.bunting.InMemoryConfig
import dev.forkhandles.bunting.int

class McpOptions(args: Array<String>) :
    Bunting(args, "A proxy to talk to an MCP server", "mcp-desktop", config =
    InMemoryConfig().apply { set("foo.bar", "configured value") }) {
    val debug by switch("Write messages to the debug log")
    val url by option("Base URL of the MCP server to connect to").required()
    val version by option().int().defaultsTo(0)
}
