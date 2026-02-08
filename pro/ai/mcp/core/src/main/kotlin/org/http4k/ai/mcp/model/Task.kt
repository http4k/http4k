package org.http4k.ai.mcp.model

import org.http4k.connect.model.TimeToLive
import se.ansman.kotshi.JsonSerializable
import java.time.Instant

@JsonSerializable
data class Task(
    val taskId: TaskId,
    val status: TaskStatus,
    val statusMessage: String? = null,
    val createdAt: Instant,
    val lastUpdatedAt: Instant,
    val ttl: TimeToLive? = null,
    val pollInterval: Int? = null
)

