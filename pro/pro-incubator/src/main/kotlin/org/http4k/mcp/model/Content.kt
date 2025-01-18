package org.http4k.mcp.model

import org.http4k.connect.model.Base64Blob
import org.http4k.mcp.MimeType

sealed interface Content {
    val type: String

    data class Text(val text: String) : Content {
        constructor(value: Any) : this(value.toString())
        override val type = "text"
    }

    data class Image(val data: Base64Blob, val mimeType: MimeType) : Content {
        override val type = "image"
    }

    data class EmbeddedResource(val resource: Resource.Content) : Content {
        override val type = "resource"
    }

    data class Unknown(override val type: String) : Content
}
