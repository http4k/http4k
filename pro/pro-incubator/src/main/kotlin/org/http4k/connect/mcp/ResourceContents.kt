package org.http4k.connect.mcp

import org.http4k.connect.model.Base64Blob
import org.http4k.core.ContentType
import org.http4k.core.Uri

sealed interface ResourceContents {
    val uri: Uri
    val mimeType: ContentType?

    data class Text(
        val text: String,
        override val uri: Uri,
        override val mimeType: ContentType? = null
    ) : ResourceContents

    data class Blob(
        val blob: Base64Blob,
        override val uri: Uri,
        override val mimeType: ContentType? = null,
    ) : ResourceContents

    data class Unknown(
        override val uri: Uri,
        override val mimeType: ContentType?,
    ) : ResourceContents
}
