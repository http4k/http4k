/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.ai.a2a.protocol.messages

import org.http4k.ai.a2a.model.Part
import org.http4k.connect.model.Base64Blob
import org.http4k.connect.model.MimeType
import org.http4k.core.Uri
import org.http4k.format.MoshiNode
import se.ansman.kotshi.JsonSerializable

@JsonSerializable
data class A2APart(
    val text: String? = null,
    val raw: Base64Blob? = null,
    val url: Uri? = null,
    val data: MoshiNode? = null,
    val metadata: Map<String, Any>? = null,
    val filename: String? = null,
    val mediaType: MimeType? = null
)

fun A2APart.toDomain(): Part = when {
    text != null -> Part.Text(text, metadata, filename, mediaType)
    raw != null -> Part.Raw(raw, metadata, filename, mediaType)
    url != null -> Part.Url(url, metadata, filename, mediaType)
    data != null -> Part.Data(data, metadata, filename, mediaType)
    else -> error("Part has no content")
}
