package org.http4k.ai.mcp.model

import org.http4k.ai.mcp.model.extension.McpAppResourceMeta
import org.http4k.ai.mcp.util.Rfc6570UriTemplateMatcher.matches
import org.http4k.connect.model.Base64Blob
import org.http4k.connect.model.MimeType
import org.http4k.core.Uri
import se.ansman.kotshi.JsonSerializable
import se.ansman.kotshi.Polymorphic
import se.ansman.kotshi.PolymorphicLabel

sealed class Resource : CapabilitySpec {
    abstract fun matches(uri: Uri): Boolean

    abstract val name: ResourceName
    abstract val size: Size?
    abstract val description: String?
    abstract val mimeType: MimeType?
    abstract val title: String?
    abstract val annotations: Annotations?
    abstract val icons: List<Icon>?
    abstract val meta: Meta?

    data class Static(
        val uri: Uri,
        override val name: ResourceName,
        override val description: String? = null,
        override val mimeType: MimeType? = null,
        override val size: Size? = null,
        override val annotations: Annotations? = null,
        override val title: String? = null,
        override val icons: List<Icon>? = null,
        override val meta: Meta? = null,
    ) : Resource() {
        constructor(
            uri: String, name: String, description: String? = null,
            mimeType: MimeType? = null, size: Size? = null,
            annotations: Annotations? = null, title: String? = null,
            icons: List<Icon>? = null, _meta: Meta? = null
        ) : this(Uri.of(uri), ResourceName.of(name), description, mimeType, size, annotations, title, icons, _meta)

        override fun matches(uri: Uri) = this.uri == uri
    }

    data class Templated(
        val uriTemplate: ResourceUriTemplate,
        override val name: ResourceName,
        override val description: String? = null,
        override val mimeType: MimeType? = null,
        override val size: Size? = null,
        override val annotations: Annotations? = null,
        override val title: String? = null,
        override val icons: List<Icon>? = null,
        override val meta: Meta? = null,
        internal val matchFn: ResourceUriTemplate.(Uri) -> Boolean = { matches(it) },
    ) : Resource() {
        constructor(
            uriTemplate: String,
            name: String,
            description: String? = null,
            mimeType: MimeType? = null,
            size: Size? = null,
            annotations: Annotations? = null,
            title: String? = null,
            icons: List<Icon>? = null,
            meta: Meta? = null,
        ) : this(
            ResourceUriTemplate.of(uriTemplate), ResourceName.of(name), description, mimeType, size, annotations,
            title, icons, meta
        )

        override fun matches(uri: Uri) = matchFn(uriTemplate, uri)
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
            override val mimeType: MimeType? = null,
            val _meta: McpAppResourceMeta? = null
        ) : Content()

        @JsonSerializable
        @PolymorphicLabel("blob")
        data class Blob(
            val blob: Base64Blob,
            override val uri: Uri,
            override val mimeType: MimeType? = null,
            val _meta: McpAppResourceMeta? = null
        ) : Content()

        @JsonSerializable
        @PolymorphicLabel("unknown")
        data class Unknown(
            override val uri: Uri,
            override val mimeType: MimeType? = null,
        ) : Content()
    }
}
