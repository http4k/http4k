package org.http4k.a2a.protocol.model

import org.http4k.core.Uri
import se.ansman.kotshi.JsonSerializable

@JsonSerializable
data class AgentCard(
    val name: String,
    val version: String,
    val description: String,
    val url: Uri,
    val skills: List<AgentSkill>,
    val capabilities: AgentCapabilities,
    val defaultInputModes: List<String>,
    val defaultOutputModes: List<String>,
    val provider: AgentProvider? = null,
    val iconUrl: Uri? = null,
    val documentationUrl: Uri? = null,
    val security: List<Map<String, List<String>>>? = null,
    val securitySchemes: Map<String, SecurityScheme>? = null,
    val supportsAuthenticatedExtendedCard: Boolean? = null
)
