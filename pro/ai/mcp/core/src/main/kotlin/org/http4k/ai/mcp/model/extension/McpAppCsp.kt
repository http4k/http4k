package org.http4k.ai.mcp.model.extension

import se.ansman.kotshi.JsonSerializable

@JsonSerializable
data class McpAppCsp(
    val connectDomains: List<CspDomain>? = null,
    val resourceDomains: List<CspDomain>? = null,
    val frameDomains: List<CspDomain>? = null,
    val baseUriDomains: List<CspDomain>? = null
)
