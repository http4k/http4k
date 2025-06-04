package org.http4k.ai.llm.tools

import org.http4k.ai.model.RequestId
import org.http4k.ai.model.ToolName
import se.ansman.kotshi.JsonSerializable

@JsonSerializable
data class ToolRequest(val id: RequestId, val name: ToolName, val arguments: Map<String, Any> = emptyMap())

