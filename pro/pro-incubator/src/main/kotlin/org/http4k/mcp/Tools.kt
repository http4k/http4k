package org.http4k.mcp

import org.http4k.connect.mcp.Tool

class Tools(list: List<ToolBinding>) {
    fun list(convert: Tool.List.Request): Tool.List.Response {
        return TODO()
    }

    fun call(convert: Tool.Call.Request): Tool.Call.Response {
        TODO("Not yet implemented")
    }
}
