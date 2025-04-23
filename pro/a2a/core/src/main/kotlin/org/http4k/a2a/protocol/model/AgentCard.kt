package org.http4k.a2a.protocol.model

import org.http4k.a2a.protocol.AgentCapabilities
import org.http4k.a2a.protocol.AgentProvider
import org.http4k.core.Uri

data class AgentCard(
    val name: String,
    val description: String? = null,
    val url: Uri,
    val provider: AgentProvider? = null,
    val version: VersionId,
    val documentationUrl: Uri? = null,
    val capabilities: AgentCapabilities,
    val authentication: AgentAuthentication? = null,

    /**
     * Default input modes supported by the agent (e.g., 'text', 'file', 'json').
     */
    val defaultInputModes: List<String> = listOf("text"),

    /**
     * Default output modes supported by the agent (e.g., 'text', 'file', 'json').
     */
    val defaultOutputModes: List<String> = listOf("text"),

    /**
     * List of specific skills offered by the agent.
     */
    val skills: List<AgentSkill>
)
