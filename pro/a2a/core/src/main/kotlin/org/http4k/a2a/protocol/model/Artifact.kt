package org.http4k.a2a.protocol.model

import se.ansman.kotshi.JsonSerializable

@JsonSerializable
data class Artifact(
    val artifactId: String,
    val parts: List<Part>,
    val name: String? = null,
    val description: String? = null,
    val metadata: Metadata = emptyMap()
)
