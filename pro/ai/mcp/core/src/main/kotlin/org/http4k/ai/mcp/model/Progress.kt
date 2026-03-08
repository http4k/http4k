/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.ai.mcp.model

import se.ansman.kotshi.JsonSerializable

@JsonSerializable
data class Progress(
    val progressToken: ProgressToken,
    val progress: Int,
    val total: Double? = null,
    val description: String? = null
)

