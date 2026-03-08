/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.ai.mcp.model

import org.http4k.connect.model.MimeType
import org.http4k.core.Uri
import se.ansman.kotshi.JsonSerializable

@JsonSerializable
data class Icon(
    val src: Uri,
    val mimeType: MimeType? = null,
    val sizes: List<IconSize> = emptyList(),
    val theme: IconTheme? = null
)

@JsonSerializable
data class IconSize(val value: String)

enum class IconTheme {
    light, dark
}
