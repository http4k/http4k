package org.http4k.ai.mcp.apps.model

import org.http4k.ai.mcp.model.Content
import se.ansman.kotshi.JsonSerializable

@JsonSerializable
data class HostToolResponse(val content: List<Content>)
