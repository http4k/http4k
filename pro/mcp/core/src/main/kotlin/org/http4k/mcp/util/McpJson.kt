package org.http4k.mcp.util

import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Moshi
import dev.forkhandles.result4k.Failure
import dev.forkhandles.result4k.Result4k
import dev.forkhandles.result4k.Success
import dev.forkhandles.result4k.get
import org.http4k.connect.model.Base64Blob
import org.http4k.connect.withAiMappings
import org.http4k.contract.jsonschema.JsonSchemaCollapser
import org.http4k.contract.jsonschema.v3.AutoJsonToJsonSchema
import org.http4k.core.Body
import org.http4k.core.Response
import org.http4k.core.Status
import org.http4k.core.Status.Companion.ACCEPTED
import org.http4k.core.Status.Companion.BAD_REQUEST
import org.http4k.core.with
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
import org.http4k.lens.LensGet
import org.http4k.lens.LensSet
import org.http4k.lens.ParamMeta.ObjectParam
import org.http4k.mcp.model.McpEntity
import org.http4k.mcp.model.McpMessageId
import org.http4k.mcp.model.Priority
import org.http4k.mcp.model.PromptName
import org.http4k.mcp.model.ResourceName
import org.http4k.mcp.model.ResourceUriTemplate
import org.http4k.mcp.model.Size
import org.http4k.mcp.model.Tool
import org.http4k.mcp.model.ToolArgLensSpec
import org.http4k.mcp.protocol.McpRpcMethod
import org.http4k.mcp.protocol.ProtocolVersion
import org.http4k.mcp.protocol.SessionId
import org.http4k.mcp.protocol.Version
import org.http4k.mcp.util.McpJson.json
import se.ansman.kotshi.KotshiJsonAdapterFactory

typealias McpNodeType = MoshiNode

object McpJson : ConfigurableMoshi(
    Moshi.Builder()
        .add(McpJsonFactory)
        .addLast(ThrowableAdapter)
        .addLast(ListAdapter)
        .addLast(SetAdapter)
        .addLast(MapAdapter)
        .addLast(MoshiNodeAdapter)
        .addLast(ErrorMessageAdapter)
        .asConfigurable()
        .withStandardMappings()
        .withAiMappings()
        .value(Base64Blob)
        .value(McpEntity)
        .value(McpRpcMethod)
        .value(McpMessageId)
        .value(Priority)
        .value(ProtocolVersion)
        .value(PromptName)
        .value(ResourceName)
        .value(ResourceUriTemplate)
        .value(SessionId)
        .value(Size)
        .value(Version)
        .done()
) {
    /**
     * Auto-marshalled lens for a tool argument. You will need Kotlin reflection on the classpath for this to work.
     */
    inline fun <reified T : Any> Tool.Arg.auto(example: T): ToolArgLensSpec<T> {
        val autoJsonToJsonSchema = AutoJsonToJsonSchema(this@McpJson)
        val jsonSchemaCollapser = JsonSchemaCollapser(this@McpJson)

        return ToolArgLensSpec(
            ObjectParam,
            LensGet { name, target -> listOf(convert<Any, T>(target.args[name]!!)) },
            LensSet { name, values, target ->
                values.fold(target) { acc, next -> target.copy(args = acc.args + (name to asJsonObject(next))) }
            },
            { jsonSchemaCollapser.collapseToNode(autoJsonToJsonSchema.toSchema(example)) }
        )
    }
}

@KotshiJsonAdapterFactory
object McpJsonFactory : JsonAdapter.Factory by KotshiMcpJsonFactory

fun Result4k<McpNodeType, McpNodeType>.asHttp() =
    when (val response = this) {
        is Success<McpNodeType> -> response.get().asHttp(ACCEPTED)
        is Failure<McpNodeType> -> response.get().asHttp(BAD_REQUEST)
    }

private fun McpNodeType.asHttp(status: Status) = when (this) {
    is MoshiNull -> Response(status)
    else -> Response(status).with(Body.json().toLens() of this)
}
