package org.http4k.mcp.model

import org.http4k.connect.model.Base64Blob
import se.ansman.kotshi.JsonSerializable
import se.ansman.kotshi.Polymorphic
import se.ansman.kotshi.PolymorphicLabel

@JsonSerializable
@Polymorphic("type")
sealed class Content {

    @JsonSerializable
    @PolymorphicLabel("text")
    data class Text(val text: String) : Content() {
        constructor(value: Any) : this(value.toString())
    }

    @JsonSerializable
    @PolymorphicLabel("image")
    data class Image(val data: Base64Blob, val mimeType: MimeType) : Content()

    @JsonSerializable
    @PolymorphicLabel("resource")
    data class EmbeddedResource(val resource: Resource.Content) : Content()
}
