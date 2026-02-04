package org.http4k.ai.mcp.protocol.messages

import org.http4k.jsonrpc.ErrorMessage

class URLElicitationRequiredError(
    val elicitations: List<McpElicitations.Request.Url>, message: String = "This request requires more information."
) : ErrorMessage(32042, message)
