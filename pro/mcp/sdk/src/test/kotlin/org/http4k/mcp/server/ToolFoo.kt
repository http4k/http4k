package org.http4k.mcp.server

import org.http4k.format.MoshiNode
import org.http4k.mcp.server.capability.ToolInput
import org.http4k.mcp.util.McpJson

class MyTool(node: MoshiNode) : ToolInput(node) {
    val a by required<Int>("the things")
}

fun main() {
    val tool = MyTool(McpJson.parse("""{"a": 123}"""))
    println(tool.a)
}
