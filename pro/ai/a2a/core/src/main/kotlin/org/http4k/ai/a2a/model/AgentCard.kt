/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.ai.a2a.model

import org.http4k.connect.model.MimeType
import org.http4k.core.Uri
import se.ansman.kotshi.JsonSerializable

@JsonSerializable
data class AgentCard(
    val name: String,
    val url: Uri,
    val version: Version,
    val description: String,
    val capabilities: AgentCapabilities = AgentCapabilities(),
    val skills: List<AgentSkill> = emptyList(),
    val defaultInputModes: List<MimeType> = emptyList(),
    val defaultOutputModes: List<MimeType> = emptyList(),
    val provider: AgentProvider? = null,
    val documentationUrl: Uri? = null,
    val iconUrl: Uri? = null,
    val supportedInterfaces: List<AgentInterface> = emptyList(),
    val securitySchemes: Map<String, SecurityScheme>? = null,
    val securityRequirements: List<SecurityRequirement>? = null,
    val signatures: List<AgentCardSignature>? = null
)

@JsonSerializable
data class AgentProvider(
    val organization: String,
    val url: Uri
)