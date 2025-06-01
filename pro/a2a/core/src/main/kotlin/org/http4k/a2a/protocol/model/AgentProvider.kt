package org.http4k.a2a.protocol.model

import org.http4k.core.Uri
import se.ansman.kotshi.JsonSerializable

@JsonSerializable
data class AgentProvider(
    val organization: Organization,
    val url: Uri
)
