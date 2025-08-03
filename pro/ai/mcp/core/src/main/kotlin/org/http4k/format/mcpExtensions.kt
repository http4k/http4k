package org.http4k.format

import org.http4k.ai.mcp.model.Elicitation
import org.http4k.ai.mcp.model.ElicitationContentLensSpec
import org.http4k.ai.mcp.model.ElicitationModel
import org.http4k.ai.mcp.util.McpJson

inline fun <reified T : ElicitationModel> Elicitation.auto(t: T, json: ConfigurableMoshi = McpJson) =
    ElicitationContentLensSpec(t, json)
