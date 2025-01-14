package org.http4k.mcp

import org.http4k.connect.mcp.Prompt
import org.http4k.connect.mcp.Tool
import org.http4k.connect.mcp.util.McpJson

class ToolBinding(val name: String, private val description: String) : McpBinding {
    fun toTool() = Tool(
        name, description, McpJson.obj(
            "type" to McpJson.string("object"),
            "properties" to McpJson.obj()
        )
    )

    fun call(arguments: Map<String, Any>) = Tool.Call.Response(listOf(Prompt.Content.Text("some text here")), false)
}
