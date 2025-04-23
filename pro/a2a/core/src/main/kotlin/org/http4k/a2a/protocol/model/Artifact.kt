package org.http4k.a2a.protocol.model

data class Artifact(
    val parts: List<Part>,
    val name: String? = null,
    val description: String? = null,
    val index: Int = 0,
    val append: Boolean? = null,
    val metadata: Metadata? = null,
    val lastChunk: Boolean? = null
)
