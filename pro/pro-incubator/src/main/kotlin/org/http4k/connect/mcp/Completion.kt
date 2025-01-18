package org.http4k.connect.mcp

import org.http4k.connect.mcp.HasMeta.Companion.default
import org.http4k.connect.mcp.model.Meta

data class Completion(val values: List<String>, val total: Int? = null, val hasMore: Boolean? = null) {

    data class Request(
        val ref: Reference,
        val argument: Argument,
        override val _meta: Meta = default
    ) : ClientMessage.Request, HasMeta {
        companion object {
            data class Argument(val name: String, val value: String)
        }
    }

    data class Response(
        val completion: Completion,
        override val _meta: Meta = default
    ) : ServerMessage.Response, HasMeta

    companion object : HasMethod {
        override val Method = McpRpcMethod.of("completion/complete")
    }
}
