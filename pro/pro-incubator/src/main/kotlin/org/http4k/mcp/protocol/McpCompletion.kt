package org.http4k.mcp.protocol

import org.http4k.mcp.model.Completion
import org.http4k.mcp.model.Meta
import org.http4k.mcp.model.Reference
import org.http4k.mcp.protocol.HasMeta.Companion.default

object McpCompletion : org.http4k.mcp.protocol.HasMethod {
    override val Method = _root_ide_package_.org.http4k.mcp.protocol.McpRpcMethod.of("completion/complete")

    data class Request(
        val ref: Reference,
        val argument: org.http4k.mcp.protocol.McpCompletion.Request.Companion.Argument,
        override val _meta: Meta = default
    ) : org.http4k.mcp.protocol.ClientMessage.Request, _root_ide_package_.org.http4k.mcp.protocol.HasMeta {
        companion object {
            data class Argument(val name: String, val value: String)
        }
    }

    data class Response(
        val completion: Completion,
        override val _meta: Meta = default
    ) : _root_ide_package_.org.http4k.mcp.protocol.ServerMessage.Response,
        _root_ide_package_.org.http4k.mcp.protocol.HasMeta
}
