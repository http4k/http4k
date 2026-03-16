package org.http4k.connect.x402.model

import org.http4k.connect.model.MimeType
import org.http4k.core.Uri
import se.ansman.kotshi.JsonSerializable

@JsonSerializable
data class ResourceInfo(
    val url: Uri,
    val description: String? = null,
    val mimeType: MimeType? = null
)
