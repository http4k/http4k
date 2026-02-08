package org.http4k.ai.mcp.model

import org.http4k.connect.model.TimeToLive
import se.ansman.kotshi.JsonSerializable

@JsonSerializable
data class TaskMeta(val ttl: TimeToLive? = null)
