package org.http4k.mcp

import org.http4k.connect.mcp.Tool
import org.http4k.connect.mcp.util.McpJson

class Tools(list: List<ToolBinding>) {
    fun list(req: Tool.List.Request): Tool.List.Response {
        return Tool.List.Response(
            listOf(
                Tool(
                    "name", "description", McpJson.obj(
                        "type" to McpJson.string("object"),
                        "properties" to McpJson.obj()
                    )
                )
            )
        )
    }

    fun call(req: Tool.Call.Request): Tool.Call.Response {
        TODO("Not yet implemented")
    }
}
