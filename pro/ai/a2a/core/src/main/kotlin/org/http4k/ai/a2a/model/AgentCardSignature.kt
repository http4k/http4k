/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.ai.a2a.model

import com.squareup.moshi.Json
import se.ansman.kotshi.JsonSerializable

@JsonSerializable
data class AgentCardSignature(
    @Json(name = "protected") val protectedHeader: String,
    val signature: String,
    val header: Map<String, Any>? = null
)
