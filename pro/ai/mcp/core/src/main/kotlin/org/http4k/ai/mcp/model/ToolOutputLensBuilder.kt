package org.http4k.ai.mcp.model

import org.http4k.ai.mcp.ToolResponse.Ok
import org.http4k.ai.mcp.protocol.ProtocolVersion
import org.http4k.ai.mcp.protocol.ProtocolVersion.Companion.DRAFT
import org.http4k.ai.mcp.protocol.ProtocolVersion.Companion.LATEST_VERSION
import org.http4k.ai.mcp.util.McpJson.asFormatString
import org.http4k.ai.mcp.util.McpJson.asJsonObject
import org.http4k.ai.mcp.util.McpNodeType
import org.http4k.lens.LensGet
import org.http4k.lens.Meta
import org.http4k.lens.ParamMeta.ObjectParam

class ToolOutputLensBuilder<OUT : Any>(
    internal val get: LensGet<Ok, OUT>,
    private val toSchema: McpCapabilityLens<Ok, *>.() -> McpNodeType
) {
    fun toLens(
        description: String? = null,
        protocolCapability: ProtocolVersion = LATEST_VERSION,
        metadata: Map<String, Any> = emptyMap()
    ) = McpCapabilityLens(
        Meta(true, "toolResponse", ObjectParam, "response", description, metadata),
        { get("response")(it).first() },
        { value, target ->
            when (protocolCapability) {
                DRAFT -> target.copy(structuredContent = asJsonObject(value), content = null)
                else -> target.copy(
                    structuredContent = asJsonObject(value),
                    content = listOf(Content.Text(asFormatString(value)))
                )
            }
        },
        { toSchema(it) }
    )
}
