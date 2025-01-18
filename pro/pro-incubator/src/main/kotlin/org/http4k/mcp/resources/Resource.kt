package org.http4k.mcp.resources

import org.http4k.connect.model.Base64Blob
import org.http4k.core.Uri
import org.http4k.mcp.MimeType

data class Resource(
    val uri: Uri,
    val name: String,
    val description: String? = null,
    val mimeType: MimeType? = null
) {
    sealed interface Content {
        val uri: Uri
        val mimeType: MimeType?

        data class Text(
            val text: String,
            override val uri: Uri,
            override val mimeType: MimeType? = null
        ) : Content

        data class Blob(
            val blob: Base64Blob,
            override val uri: Uri,
            override val mimeType: MimeType? = null,
        ) : Content

        data class Unknown(
            override val uri: Uri,
            override val mimeType: MimeType? = null,
        ) : Content
    }
}
