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
