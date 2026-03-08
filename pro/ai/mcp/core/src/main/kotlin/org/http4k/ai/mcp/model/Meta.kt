/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
@file:OptIn(ExperimentalKotshiApi::class)

package org.http4k.ai.mcp.model

import org.http4k.ai.mcp.model.apps.McpAppMeta
import se.ansman.kotshi.ExperimentalKotshiApi
import se.ansman.kotshi.JsonProperty
import se.ansman.kotshi.JsonSerializable

@JsonSerializable
data class Meta(
    val progressToken: ProgressToken? = null,
    @JsonProperty("io.modelcontextprotocol/related-task")
    val relatedTask: RelatedTaskMetadata? = null,
    val ui: McpAppMeta? = null,
    val traceparent: String? = null,
    val tracestate: String? = null,
    val baggage: String? = null,
) {
    companion object {
        val default = Meta()
    }
}

