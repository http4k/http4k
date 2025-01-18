package org.http4k.mcp.model

import org.http4k.connect.model.Base64Blob
import org.http4k.core.Uri

sealed interface Resource {
    fun matches(uri: Uri): Boolean

    val name: String
    val description: String?
    val mimeType: MimeType?

    data class Static(
        val uri: Uri,
        override val name: String,
        override val description: String? = null,
        override val mimeType: MimeType? = null
    ) : Resource {
        override fun matches(uri: Uri) = this.uri == uri
    }

    data class Templated(
        val uriTemplate: Uri,
        override val name: String,
        override val description: String? = null,
        override val mimeType: MimeType? = null,
        private val matchFn: (Uri) -> Boolean = { uriTemplate.authority == it.authority }
    ) : Resource {
        override fun matches(uri: Uri) = matchFn(uri)
    }

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
