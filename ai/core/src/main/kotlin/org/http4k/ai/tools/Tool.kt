package org.http4k.ai.tools

import org.http4k.ai.model.ToolName
import org.http4k.ai.util.AiJsonNode

data class Tool(
    val name: ToolName,
    val description: String? = null,
    val inputSchema: AiJsonNode? = null,
    val outputSchema: AiJsonNode? = null
)
