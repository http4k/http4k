package org.http4k.ai.llm.tools

import org.http4k.ai.llm.model.Message.ToolResult
import org.http4k.ai.model.RequestId
import org.http4k.ai.model.ToolName

data class ToolResponse(val result: ToolResult) {
    constructor(id: RequestId, tool: ToolName, text: String) : this(ToolResult(id, tool, text))
}
