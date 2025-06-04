package org.http4k.ai.llm.tools

import org.http4k.ai.model.ToolName

data class LLMTool(
    val name: ToolName,
    val description: String,
    val inputSchema: Map<String, Any> = emptyMap(),
    val outputSchema: Map<String, Any>? = null
) {
    constructor(
        name: String,
        description: String,
        inputSchema: Map<String, Any> = emptyMap(),
        outputSchema: Map<String, Any>? = null
    ) : this(ToolName.of(name), description, inputSchema, outputSchema)
}
