/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.ai.mcp.stateless

import org.http4k.ai.mcp.stateless.Client.Companion.NoOp
import org.http4k.ai.mcp.stateless.model.McpUri
import org.http4k.ai.mcp.stateless.model.Meta
import org.http4k.ai.mcp.stateless.model.Meta.Companion.default
import org.http4k.ai.mcp.stateless.model.Resource
import org.http4k.ai.mcp.stateless.model.TtlMs
import org.http4k.core.Request

/**
 * A resource handler is responsible for loading the content of a Resource
 */
typealias ResourceHandler = (ResourceRequest) -> ResourceResponse

fun interface ResourceFilter {
    operator fun invoke(handler: ResourceHandler): ResourceHandler
    companion object
}

val ResourceFilter.Companion.NoOp: ResourceFilter get() = ResourceFilter { it }

fun ResourceFilter.then(next: ResourceFilter): ResourceFilter = ResourceFilter { this(next(it)) }

fun ResourceFilter.then(next: ResourceHandler): ResourceHandler = this(next)

data class ResourceRequest(
    val uri: McpUri,
    val meta: Meta = default,
    val client: Client = NoOp,
    val connectRequest: Request? = null,
    val inputResponses: Map<String, ElicitationResponse> = emptyMap(),
    val requestState: String? = null,
)

sealed interface ResourceResponse {
    val meta: Meta

    data class Ok(
        val list: List<Resource.Content>,
        val ttlMs: TtlMs = TtlMs.of(0),
        override val meta: Meta = default
    ) : ResourceResponse {
        constructor(vararg content: Resource.Content, meta: Meta = default) : this(content.toList(), meta = meta)
    }

    data class Error(val message: String, override val meta: Meta = default) : ResourceResponse

    data class InputRequired(
        val inputRequests: Map<String, ElicitationRequest>,
        val requestState: String? = null,
        override val meta: Meta = default
    ) : ResourceResponse
}
