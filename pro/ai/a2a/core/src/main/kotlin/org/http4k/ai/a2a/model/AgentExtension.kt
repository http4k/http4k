/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.ai.a2a.model

import org.http4k.core.Uri
import se.ansman.kotshi.JsonSerializable

@JsonSerializable
data class AgentExtension(
    val uri: Uri,
    val description: String? = null,
    val required: Boolean = false,
    val params: Map<String, Any>? = null
)
