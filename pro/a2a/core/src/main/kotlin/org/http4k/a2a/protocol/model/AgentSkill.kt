package org.http4k.a2a.protocol.model

import se.ansman.kotshi.JsonSerializable

@JsonSerializable
data class AgentSkill(
    val id: SkillId,
    val name: String,
    val description: String,
    val tags: List<String>,
    val examples: List<String>? = null,
    val inputModes: List<String>? = null,
    val outputModes: List<String>? = null
)
