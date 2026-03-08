/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.ai.mcp.model.apps

import org.http4k.ai.mcp.model.Domain
import se.ansman.kotshi.JsonSerializable

@JsonSerializable
data class Csp(
    val connectDomains: List<Domain>? = null,
    val resourceDomains: List<Domain>? = null,
    val frameDomains: List<Domain>? = null,
    val baseUriDomains: List<Domain>? = null
)
