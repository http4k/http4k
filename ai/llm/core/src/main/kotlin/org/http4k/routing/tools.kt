package org.http4k.routing

import org.http4k.ai.llm.tools.LLMTool
import org.http4k.ai.llm.tools.LLMTools
import org.http4k.ai.llm.tools.ToolHandler

infix fun LLMTool.bind(toolHandler: ToolHandler) = RoutingToolHandler(listOf(this to toolHandler))

fun tools(vararg tools: Pair<LLMTool, ToolHandler>) = RoutingToolHandler(tools.toList())

fun tools(vararg toolCollections: LLMTools) = CompositeLLMTools(toolCollections.toList())
