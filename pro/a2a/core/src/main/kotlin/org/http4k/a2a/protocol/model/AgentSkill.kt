package org.http4k.a2a.protocol.model

data class AgentSkill(
    val id: SkillId,
    val name: String,
    val description: String? = null,
    val tags: List<SkillTag>? = null,
    val examples: List<String>? = null,

    /**
     * Optional list of input modes supported by this skill, overriding agent defaults.
     */
    val inputModes: List<String>? = null,

    /**
     * Optional list of output modes supported by this skill, overriding agent defaults.
     */
    val outputModes: List<String>? = null
)
