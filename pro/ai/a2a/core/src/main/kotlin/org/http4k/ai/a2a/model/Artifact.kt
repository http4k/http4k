package org.http4k.ai.a2a.model

import se.ansman.kotshi.JsonSerializable

@JsonSerializable
data class Artifact(
    val artifactId: ArtifactId,
    val parts: List<Part>,
    val name: String? = null,
    val description: String? = null,
    val metadata: Map<String, Any>? = null
)
