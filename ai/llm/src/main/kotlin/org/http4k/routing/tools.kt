package org.http4k.routing

import org.http4k.ai.llm.tools.LLMTool
import org.http4k.ai.llm.tools.ToolHandler
import org.http4k.ai.llm.tools.Tools

infix fun LLMTool.bind(toolHandler: ToolHandler) = RoutingToolHandler(listOf(this to toolHandler))

fun tools(vararg tools: Pair<LLMTool, ToolHandler>) = RoutingToolHandler(tools.toList())

fun tools(vararg toolCollections: Tools) = CompositeTools(toolCollections.toList())
