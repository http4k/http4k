package org.http4k.a2a.protocol.model

import org.http4k.connect.model.Base64Blob
import org.http4k.connect.model.MimeType

sealed class FileContent {
    abstract val name: String?
    abstract val mimeType: MimeType?

    data class Bytes(
        val bytes: Base64Blob,
        override val name: String? = null,
        override val mimeType: MimeType? = null
    ) : FileContent()

    data class Uri(
        val uri: org.http4k.core.Uri,
        override val name: String? = null,
        override val mimeType: MimeType? = null
    ) : FileContent()
}
