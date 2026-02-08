package org.http4k.ai.mcp.model

import se.ansman.kotshi.JsonSerializable

@JsonSerializable
data class RelatedTaskMetadata(
    val taskId: TaskId
)
