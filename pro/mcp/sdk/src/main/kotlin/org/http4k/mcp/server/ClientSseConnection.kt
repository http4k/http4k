package org.http4k.mcp.server

import org.http4k.mcp.processing.McpMessageHandler
import org.http4k.sse.Sse

class ClientSseConnection<NODE : Any>(val sse: Sse, val handler: McpMessageHandler<NODE>) {

}
