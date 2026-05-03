/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.ai.a2a.model

import org.http4k.connect.model.MimeType
import se.ansman.kotshi.JsonSerializable

@JsonSerializable
data class AgentSkill(
    val id: SkillId,
    val name: String,
    val description: String,
    val tags: List<String> = emptyList(),
    val examples: List<String>? = null,
    val inputModes: List<MimeType>? = null,
    val outputModes: List<MimeType>? = null,
    val securityRequirements: List<SecurityRequirement>? = null
)
