package org.http4k.mcp.processing

import org.http4k.jsonrpc.ErrorMessage

class McpException(val error: ErrorMessage, cause: Throwable? = null) : Exception(error.message, cause)
