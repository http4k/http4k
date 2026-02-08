package org.http4k.ai.mcp.model

import org.http4k.ai.mcp.model.apps.McpAppResourceMeta
import org.http4k.ai.model.ToolName
import org.http4k.connect.model.Base64Blob
import org.http4k.connect.model.MimeType
import org.http4k.core.Uri
import se.ansman.kotshi.JsonSerializable
import se.ansman.kotshi.Polymorphic
import se.ansman.kotshi.PolymorphicLabel

@JsonSerializable
@Polymorphic("type")
sealed class Content {
    @JsonSerializable
    @PolymorphicLabel("audio")
    data class Audio(val data: Base64Blob, val mimeType: MimeType, val annotations: Annotations? = null) : Content()

    @JsonSerializable
    @PolymorphicLabel("image")
    data class Image(val data: Base64Blob, val mimeType: MimeType, val annotations: Annotations? = null) : Content()

    @JsonSerializable
    @PolymorphicLabel("resource")
    data class EmbeddedResource(val resource: Resource.Content, val annotations: Annotations? = null) : Content()

    @JsonSerializable
    @PolymorphicLabel("resource_link")
    data class ResourceLink(
        val uri: Uri,
        val name: ResourceName,
        val title: String? = null,
        val description: String? = null,
        val mimeType: MimeType? = null,
        val annotations: Annotations? = null
    ) : Content()

    @JsonSerializable
    @PolymorphicLabel("text")
    data class Text(val text: String, val annotations: Annotations? = null) : Content() {
        constructor(value: Any, annotations: Annotations? = null) : this(value.toString(), annotations)
    }

    @JsonSerializable
    @PolymorphicLabel("tool_use")
    data class ToolUse(
        val id: ToolUseId,
        val name: ToolName,
        val input: Map<String, Any>,
        val _meta: Meta? = null
    ) : Content()

    @JsonSerializable
    @PolymorphicLabel("tool_result")
    data class ToolResult(
        val toolUseId: ToolUseId,
        val content: List<Content>? = null,
        val structuredContent: Map<String, Any>? = null,
        val isError: Boolean? = false,
        val _meta: Meta? = null
    ) : Content()

    @JsonSerializable
    data class Meta(
        val ui: McpAppResourceMeta? = null
    )
}
