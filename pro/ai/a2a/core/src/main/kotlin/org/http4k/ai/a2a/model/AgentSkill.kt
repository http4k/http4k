package org.http4k.ai.a2a.model

import se.ansman.kotshi.JsonSerializable

@JsonSerializable
data class AgentSkill(
    val id: SkillId,
    val name: String,
    val description: String? = null,
    val tags: List<String>? = null,
    val examples: List<String>? = null
)
