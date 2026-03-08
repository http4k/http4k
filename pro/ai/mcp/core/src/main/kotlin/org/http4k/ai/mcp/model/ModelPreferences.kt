/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.ai.mcp.model

import se.ansman.kotshi.JsonSerializable

@JsonSerializable
data class ModelPreferences(
    val hints: List<ModelHint>? = null,
    val costPriority: Priority? = null,
    val speedPriority: Priority? = null,
    val intelligencePriority: Priority? = null
)

