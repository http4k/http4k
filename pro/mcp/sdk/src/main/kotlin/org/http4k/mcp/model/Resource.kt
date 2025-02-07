package org.http4k.mcp.model

import org.http4k.connect.model.Base64Blob
import org.http4k.core.Uri
import se.ansman.kotshi.JsonSerializable
import se.ansman.kotshi.Polymorphic
import se.ansman.kotshi.PolymorphicLabel

sealed class Resource : CapabilitySpec  {
    abstract fun matches(uri: Uri): Boolean

    abstract val name: String
    abstract val description: String?
    abstract val mimeType: MimeType?

    data class Static(
        val uri: Uri,
        override val name: String,
        override val description: String? = null,
        override val mimeType: MimeType? = null
    ) : Resource() {
        override fun matches(uri: Uri) = this.uri == uri
    }

    data class Templated(
        val uriTemplate: Uri,
        override val name: String,
        override val description: String? = null,
        override val mimeType: MimeType? = null,
        internal val matchFn: ((Uri) -> Boolean) = { uriTemplate.authority == it.authority }
    ) : Resource() {
        override fun matches(uri: Uri) = matchFn(uri)
    }

    @JsonSerializable
    @Polymorphic("type")
    sealed class Content {
        abstract val uri: Uri
        abstract val mimeType: MimeType?

        @JsonSerializable
        @PolymorphicLabel("text")
        data class Text(
            val text: String,
            override val uri: Uri,
            override val mimeType: MimeType? = null
        ) : Content()

        @JsonSerializable
        @PolymorphicLabel("blob")
        data class Blob(
            val blob: Base64Blob,
            override val uri: Uri,
            override val mimeType: MimeType? = null,
        ) : Content()

        @JsonSerializable
        @PolymorphicLabel("unknown")
        data class Unknown(
            override val uri: Uri,
            override val mimeType: MimeType? = null,
        ) : Content()
    }
}
