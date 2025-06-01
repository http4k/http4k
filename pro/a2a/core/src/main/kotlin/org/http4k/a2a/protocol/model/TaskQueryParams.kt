package org.http4k.a2a.protocol.model

import se.ansman.kotshi.JsonSerializable

@JsonSerializable
data class TaskQueryParams(
    val id: String,
    val historyLength: Int? = null,
    val metadata: Metadata? = null
)
