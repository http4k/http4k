/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.ai.mcp.model

import se.ansman.kotshi.JsonSerializable

@JsonSerializable
data class Completion(val values: List<String>, val total: Int? = null, val hasMore: Boolean? = null) {
    constructor(vararg values: String, total: Int? = null, hasMore: Boolean? = null) : this(
        values.toList(), total,
        hasMore
    )
}
