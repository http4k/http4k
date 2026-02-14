package org.http4k.ai.a2a.model

import org.http4k.connect.model.Base64Blob
import org.http4k.connect.model.MimeType
import org.http4k.core.Uri
import se.ansman.kotshi.JsonSerializable
import se.ansman.kotshi.Polymorphic
import se.ansman.kotshi.PolymorphicLabel

@JsonSerializable
@Polymorphic("type")
sealed class Part {
    @JsonSerializable
    @PolymorphicLabel("text")
    data class Text(val text: String) : Part()

    @JsonSerializable
    @PolymorphicLabel("file")
    data class File(
        val data: Base64Blob? = null,
        val uri: Uri? = null,
        val mimeType: MimeType? = null,
        val name: String? = null
    ) : Part()

    @JsonSerializable
    @PolymorphicLabel("data")
    data class Data(val data: Map<String, Any>) : Part()
}
