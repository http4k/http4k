package org.http4k.format

import org.http4k.ai.mcp.model.Elicitation
import org.http4k.ai.mcp.model.ElicitationLensSpec
import org.http4k.ai.mcp.util.McpJson

inline fun <reified T : Any> Elicitation.auto(t: T, json: ConfigurableMoshi = McpJson) = ElicitationLensSpec.map(
    { json.asA<T>(json.asFormatString(it)) },
    { json.asJsonObject(it) },
)
