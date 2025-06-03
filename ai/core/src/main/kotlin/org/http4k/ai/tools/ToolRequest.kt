package org.http4k.ai.tools

import org.http4k.ai.model.RequestId
import org.http4k.ai.model.ToolName

data class ToolRequest(val id: RequestId, val name: ToolName, val arguments: Map<String, Any> = emptyMap())
