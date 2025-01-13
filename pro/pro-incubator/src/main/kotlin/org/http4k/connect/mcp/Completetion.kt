package org.http4k.connect.mcp

import org.http4k.connect.mcp.HasMeta.Companion.default

object Completetion : HasMethod {
    override val Method = McpRpcMethod.of("completion/complete")

    data class Request(
        val ref: Reference,
        val argument: Argument,
        override val _meta: Meta = default
    ) : ClientRequest, HasMeta {
        companion object {
            data class Argument(val name: String, val value: String)
        }
    }

    data class Response(
        val completion: Completion,
        override val _meta: Meta = default
    ) : ServerResponse, HasMeta {
        data class Completion(
            val values: List<String>,
            val total: Int? = null,
            val hasMore: Boolean? = null,
        )
    }
}
