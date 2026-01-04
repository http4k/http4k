package org.http4k.ai.mcp.model

import org.http4k.core.Uri
import org.http4k.connect.model.MimeType
import se.ansman.kotshi.JsonSerializable

@JsonSerializable
data class Icon(
    val src: Uri,
    val mimeType: MimeType? = null,
    val sizes: List<IconSize> = emptyList(),
    val theme: IconTheme? = null
)

@JsonSerializable
data class IconSize(val value: String)

enum class IconTheme {
    light, dark
}
