package org.http4k.a2a.protocol.model

import org.http4k.connect.model.Role

data class Message(
    val role: Role,
    val parts: List<Part>,
    val metadata: Metadata? = null
)
