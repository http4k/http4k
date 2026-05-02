/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.ai.a2a.model

import org.http4k.core.Uri
import se.ansman.kotshi.JsonSerializable

@JsonSerializable
data class AgentCard(
    val name: String,
    val url: Uri,
    val version: String,
    val description: String? = null,
    val capabilities: AgentCapabilities? = null,
    val skills: List<AgentSkill>? = null,
    val defaultInputModes: List<ContentMode>? = null,
    val defaultOutputModes: List<ContentMode>? = null,
    val provider: AgentProvider? = null,
    val documentationUrl: Uri? = null,
    val authentication: AgentAuthentication? = null
)

@JsonSerializable
data class AgentProvider(
    val organization: String,
    val url: Uri? = null
)

@JsonSerializable
data class AgentAuthentication(
    val schemes: List<AuthScheme>,
    val credentials: String? = null
)
