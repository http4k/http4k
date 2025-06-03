package org.http4k.ai.model

import org.http4k.connect.model.Base64Blob
import org.http4k.connect.model.MimeType
import org.http4k.core.Uri
import se.ansman.kotshi.JsonSerializable
import se.ansman.kotshi.Polymorphic
import se.ansman.kotshi.PolymorphicLabel

@JsonSerializable
@Polymorphic("type")
sealed class Resource {
    abstract val mimeType: MimeType?

    @JsonSerializable
    @PolymorphicLabel("ref")
    data class Ref(val uri: Uri, override val mimeType: MimeType? = null) : Resource()

    @JsonSerializable
    @PolymorphicLabel("binary")
    data class Binary(val content: Base64Blob, override val mimeType: MimeType? = null) : Resource()
}
