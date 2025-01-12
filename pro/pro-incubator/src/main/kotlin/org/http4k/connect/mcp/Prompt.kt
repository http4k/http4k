package org.http4k.connect.mcp

import org.http4k.connect.mcp.HasMeta.Companion.default
import org.http4k.connect.mcp.McpRpcMethod.Companion.of
import org.http4k.connect.model.Base64Blob
import org.http4k.core.ContentType

data class Prompt(
    val name: String,
    val description: String? = null,
    val arguments: kotlin.collections.List<Argument>? = null,
) {
    data class Message(val role: Role, val content: Content)

    sealed interface Content {
        val type: String

        data class Text(val text: String) : Content {
            override val type = "text"
        }

        data class Image(
            val data: Base64Blob,
            val mimeType: ContentType,
        ) : Content {
            override val type = "image"
        }

        data class EmbeddedResource(val resource: ResourceContents) : Content {
            override val type = "resource"
        }

        data class Unknown(override val type: String) : Content
    }

    data class Argument(
        val name: String,
        val description: String? = null,
        val required: Boolean? = null,
    )

    object Get : HasMethod {
        override val method = of("prompts/get")

        data class Request(
            val name: String,
            val arguments: Map<String, String>? = null,
            override val _meta: Meta = default
        ) : ClientRequest, HasMeta

        class Response(
            val messages: kotlin.collections.List<Message>,
            val description: String? = null,
            override val _meta: Meta = default
        ) : ServerResponse, HasMeta
    }

    object List : HasMethod {
        override val method = of("prompts/list")

        data class Request(override val _meta: Meta = default) : ClientRequest, HasMeta

        data class Response(
            val prompts: kotlin.collections.List<Prompt>,
            override val _meta: Meta = default
        ) : ServerResponse, HasMeta

        data object Notification : ServerNotification {
            override val method = of("notifications/prompts/list_changed")
        }
    }
}
