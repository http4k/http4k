package org.http4k.ai.mcp.protocol

import org.http4k.jsonrpc.ErrorMessage

class McpException(val error: ErrorMessage, cause: Throwable? = null) : Exception(error.message, cause)
