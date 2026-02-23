package org.http4k.ai.mcp.util

import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Moshi
import dev.forkhandles.result4k.Failure
import dev.forkhandles.result4k.Result4k
import dev.forkhandles.result4k.Success
import dev.forkhandles.result4k.get
import org.http4k.ai.mcp.model.ElicitationId
import org.http4k.ai.mcp.model.McpEntity
import org.http4k.ai.mcp.model.McpMessageId
import org.http4k.ai.mcp.model.Priority
import org.http4k.ai.mcp.model.PromptName
import org.http4k.ai.mcp.model.ResourceName
import org.http4k.ai.mcp.model.ResourceUriTemplate
import org.http4k.ai.mcp.model.Size
import org.http4k.ai.mcp.model.TaskId
import org.http4k.ai.mcp.model.Tool
import org.http4k.ai.mcp.model.ToolArgLensSpec
import org.http4k.ai.mcp.model.ToolOutputLensBuilder
import org.http4k.ai.mcp.model.Domain
import org.http4k.ai.mcp.model.ToolUseId
import org.http4k.ai.mcp.protocol.McpRpcMethod
import org.http4k.ai.mcp.protocol.ProtocolVersion
import org.http4k.ai.mcp.protocol.SessionId
import org.http4k.ai.mcp.protocol.Version
import org.http4k.ai.util.withAiMappings
import org.http4k.connect.model.Base64Blob
import org.http4k.connect.model.TimeToLive
import org.http4k.contract.jsonschema.JsonSchemaCollapser
import org.http4k.contract.jsonschema.v3.AutoJsonToJsonSchema
import org.http4k.core.ContentType
import org.http4k.core.Response
import org.http4k.core.Status
import org.http4k.core.Status.Companion.BAD_REQUEST
import org.http4k.core.with
import org.http4k.format.AutoMappingConfiguration
import org.http4k.format.ConfigurableMoshi
import org.http4k.format.ListAdapter
import org.http4k.format.MapAdapter
import org.http4k.format.MoshiNode
import org.http4k.format.MoshiNodeAdapter
import org.http4k.format.MoshiNull
import org.http4k.format.SetAdapter
import org.http4k.format.ThrowableAdapter
import org.http4k.format.asConfigurable
import org.http4k.format.value
import org.http4k.format.withStandardMappings
import org.http4k.lens.Header
import org.http4k.lens.LensGet
import org.http4k.lens.LensSet
import org.http4k.lens.ParamMeta.ObjectParam
import org.http4k.sse.SseMessage.Event
import se.ansman.kotshi.KotshiJsonAdapterFactory

typealias McpNodeType = MoshiNode

/**
 * Builder for MCP JSON marshalling. You can pass your own [JsonAdapter.Factory] and configuration block to this class.
 */
abstract class ConfigurableMcpJson(
    customJsonFactory: JsonAdapter.Factory = JsonAdapter.Factory { _, _, _ -> null },
    customMappings: AutoMappingConfiguration<Moshi.Builder>.() -> AutoMappingConfiguration<Moshi.Builder> = { this }
) : ConfigurableMoshi(
    Moshi.Builder()
        .add(McpJsonFactory)
        .addLast(ThrowableAdapter)
        .addLast(ListAdapter)
        .addLast(SetAdapter)
        .addLast(MapAdapter)
        .addLast(MoshiNodeAdapter)
        .addLast(ErrorMessageAdapter)
        .addLast(customJsonFactory)
        .asConfigurable()
        .apply { customMappings() }
        .withMcpMappings()
        .done()
) {
    /**
     * Auto-marshalled lens for a tool argument. You will need Kotlin reflection on the classpath for this to work.
     */
    inline fun <reified T : Any> Tool.Arg.auto(example: T): ToolArgLensSpec<T> {
        val autoJsonToJsonSchema = AutoJsonToJsonSchema(this@ConfigurableMcpJson)
        val jsonSchemaCollapser = JsonSchemaCollapser(this@ConfigurableMcpJson)

        return ToolArgLensSpec(
            ObjectParam,
            LensGet { name, target -> listOf(convert<Any, T>(target.args[name]!!)) },
            LensSet { name, values, target ->
                values.fold(target) { acc, next -> target.copy(args = acc.args + (name to asJsonObject(next))) }
            },
            { jsonSchemaCollapser.collapseToNode(autoJsonToJsonSchema.toSchema(example)) }
        )
    }

    /**
     * Auto-marshalled lens for a tool output. You will need Kotlin reflection on the classpath for this to work.
     */
    @Suppress("UnusedReceiverParameter")
    inline fun <reified T : Any> Tool.Output.auto(example: T): ToolOutputLensBuilder<T> {
        val autoJsonToJsonSchema = AutoJsonToJsonSchema(this@ConfigurableMcpJson)
        val jsonSchemaCollapser = JsonSchemaCollapser(this@ConfigurableMcpJson)

        return ToolOutputLensBuilder(
            LensGet { _, target -> listOf(convert(target.structuredContent!!)) },
            { jsonSchemaCollapser.collapseToNode(autoJsonToJsonSchema.toSchema(example)) }
        )
    }
}

@KotshiJsonAdapterFactory
object McpJsonFactory : JsonAdapter.Factory by KotshiMcpJsonFactory

fun <T> AutoMappingConfiguration<T>.withMcpMappings() = apply {
    withStandardMappings()
    withAiMappings()
    value(Base64Blob)
    value(Domain)
    value(ElicitationId)
    value(McpEntity)
    value(McpRpcMethod)
    value(McpMessageId)
    value(Priority)
    value(ProtocolVersion)
    value(PromptName)
    value(ResourceName)
    value(ResourceUriTemplate)
    value(SessionId)
    value(Size)
    value(TaskId)
    value(TimeToLive)
    value(ToolUseId)
    value(Version)
}

fun Result4k<McpNodeType, McpNodeType>.asHttp(status: Status) =
    when (val response = this) {
        is Success<McpNodeType> -> response.get().asHttp(status)
        is Failure<McpNodeType> -> response.get().asHttp(BAD_REQUEST)
    }

private fun McpNodeType.asHttp(status: Status) = when (this) {
    is MoshiNull -> Response(status)
    else -> Response(status)
        .with(Header.CONTENT_TYPE of ContentType.TEXT_EVENT_STREAM)
        .body(Event("message", McpJson.asFormatString(this)).toMessage())
}
