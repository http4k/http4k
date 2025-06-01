package org.http4k.a2a.protocol.model

import org.http4k.connect.model.Base64Blob
import org.http4k.connect.model.MimeType
import org.http4k.core.Uri
import se.ansman.kotshi.JsonSerializable
import se.ansman.kotshi.Polymorphic
import se.ansman.kotshi.PolymorphicLabel

@JsonSerializable
@Polymorphic("kind")
sealed interface Part {
    val metadata: Metadata?
}

@JsonSerializable
@PolymorphicLabel("text")
data class TextPart(
    val text: String, override val metadata: Metadata? = null
) : Part

@JsonSerializable
@PolymorphicLabel("file")
data class FilePart(val file: File, override val metadata: Metadata? = null) : Part

@JsonSerializable
@PolymorphicLabel("data")
data class DataPart(
    val data: Map<String, Any>,
    override val metadata: Metadata? = null
) : Part

@JsonSerializable
sealed interface File {
    val name: String?
    val mimeType: MimeType?
}

@JsonSerializable
data class FileWithBytes(
    val bytes: Base64Blob,
    override val name: String? = null,
    override val mimeType: MimeType? = null
) : File

@JsonSerializable
data class FileWithUri(
    val uri: Uri,
    override val name: String? = null,
    override val mimeType: MimeType? = null
) : File
