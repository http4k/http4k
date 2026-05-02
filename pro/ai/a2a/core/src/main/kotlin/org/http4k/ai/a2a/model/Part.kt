/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.ai.a2a.model

import org.http4k.ai.a2a.protocol.messages.A2APart
import org.http4k.connect.model.Base64Blob
import org.http4k.connect.model.MimeType
import org.http4k.core.Uri
import org.http4k.format.MoshiNode

sealed class Part(
    open val metadata: Map<String, Any>?,
    open val filename: String?,
    open val mediaType: MimeType?
) {
    data class Text(
        val text: String,
        override val metadata: Map<String, Any>? = null,
        override val filename: String? = null,
        override val mediaType: MimeType? = null
    ) : Part(metadata, filename, mediaType)

    data class Raw(
        val raw: Base64Blob,
        override val metadata: Map<String, Any>? = null,
        override val filename: String? = null,
        override val mediaType: MimeType? = null
    ) : Part(metadata, filename, mediaType)

    data class Url(
        val url: Uri,
        override val metadata: Map<String, Any>? = null,
        override val filename: String? = null,
        override val mediaType: MimeType? = null
    ) : Part(metadata, filename, mediaType)

    data class Data(
        val data: MoshiNode,
        override val metadata: Map<String, Any>? = null,
        override val filename: String? = null,
        override val mediaType: MimeType? = null
    ) : Part(metadata, filename, mediaType)
}

fun Part.toWire(): A2APart = when (this) {
    is Part.Text -> A2APart(text = text, metadata = metadata, filename = filename, mediaType = mediaType)
    is Part.Raw -> A2APart(raw = raw, metadata = metadata, filename = filename, mediaType = mediaType)
    is Part.Url -> A2APart(url = url, metadata = metadata, filename = filename, mediaType = mediaType)
    is Part.Data -> A2APart(data = data, metadata = metadata, filename = filename, mediaType = mediaType)
}
