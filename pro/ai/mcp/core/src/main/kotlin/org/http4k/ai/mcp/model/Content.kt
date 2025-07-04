package org.http4k.ai.mcp.model

import org.http4k.connect.model.Base64Blob
import org.http4k.connect.model.MimeType
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
    @PolymorphicLabel("text")
    data class Text(val text: String, val annotations: Annotations? = null) : Content() {
        constructor(value: Any, annotations: Annotations? = null) : this(value.toString(), annotations)
    }
}
