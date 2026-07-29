/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.ai.mcp.stateless.util

import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.JsonReader
import com.squareup.moshi.JsonWriter
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types.getRawType
import org.http4k.ai.mcp.stateless.model.Domain
import org.http4k.ai.mcp.stateless.model.McpEntity
import org.http4k.ai.mcp.stateless.model.McpMessageId
import org.http4k.ai.mcp.stateless.model.Meta
import org.http4k.ai.mcp.stateless.model.MetaField
import org.http4k.ai.mcp.stateless.model.Priority
import org.http4k.ai.mcp.stateless.model.PromptName
import org.http4k.ai.mcp.stateless.model.ResourceName
import org.http4k.ai.mcp.stateless.model.ResourceUriTemplate
import org.http4k.ai.mcp.stateless.model.Size
import org.http4k.ai.mcp.stateless.model.TaskId
import org.http4k.ai.mcp.stateless.model.Tool
import org.http4k.ai.mcp.stateless.model.ToolArgLensSpec
import org.http4k.ai.mcp.stateless.model.ToolOutputLensBuilder
import org.http4k.ai.mcp.stateless.model.ToolUseId
import org.http4k.ai.mcp.stateless.model.TtlMs
import org.http4k.ai.mcp.stateless.protocol.McpRpcMethod
import org.http4k.ai.mcp.stateless.protocol.ProtocolVersion
import org.http4k.ai.mcp.stateless.protocol.Version
import org.http4k.ai.util.withAiMappings
import org.http4k.connect.model.Base64Blob
import org.http4k.connect.model.TimeToLive
import org.http4k.contract.jsonschema.JsonSchemaCollapser
import org.http4k.contract.jsonschema.v3.AutoJsonToJsonSchema
import org.http4k.format.ArrayItemsJsonAdapterFactory
import org.http4k.format.AutoMappingConfiguration
import org.http4k.format.AutoMarshallingJson
import org.http4k.format.ConfigurableMoshi
import org.http4k.format.ListAdapter
import org.http4k.format.MapAdapter
import org.http4k.format.MoshiNode
import org.http4k.format.MoshiNodeAdapter
import org.http4k.format.MoshiObject
import org.http4k.format.SchemaNodeJsonAdapterFactory
import org.http4k.format.SetAdapter
import org.http4k.format.ThrowableAdapter
import org.http4k.format.asConfigurable
import org.http4k.format.unwrap
import org.http4k.format.value
import org.http4k.format.withStandardMappings
import org.http4k.format.wrap
import org.http4k.lens.LensGet
import org.http4k.lens.LensSet
import org.http4k.lens.ParamMeta.ObjectParam
import org.http4k.lens.stateless.MetaKey
import org.http4k.lens.stateless.MetaLensSpec
import se.ansman.kotshi.KotshiJsonAdapterFactory
import java.lang.reflect.Type

typealias McpNodeType = MoshiNode

/**
 * Builder for MCP JSON marshalling. You can pass your own [JsonAdapter.Factory] and configuration block to this class.
 */
@Suppress("UnnecessaryAbstractClass")
abstract class ConfigurableMcpJson(
    customJsonFactory: JsonAdapter.Factory = JsonAdapter.Factory { _, _, _ -> null },
    customMappings: AutoMappingConfiguration<Moshi.Builder>.() -> AutoMappingConfiguration<Moshi.Builder> = { this }
) : ConfigurableMoshi(
    Moshi.Builder()
        .add(McpJsonFactory)
        .add(MetaAdapter)
        .add(IntegralNumberAdapter)
        .addLast(SchemaNodeJsonAdapterFactory)
        .addLast(ArrayItemsJsonAdapterFactory)
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
            LensGet { name, target -> listOf(convert<Any, T>(target.args.getValue(name))) },
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

        return ToolOutputLensBuilder(this@ConfigurableMcpJson,
            LensGet { _, target -> listOf(convert(requireNotNull(target.structuredContent))) },
            { jsonSchemaCollapser.collapseToNode(autoJsonToJsonSchema.toSchema(example)) }
        )
    }
}

inline fun <reified T : Any> MetaKey.auto(field: MetaField<T>, json: AutoMarshallingJson<McpNodeType> = McpJson) = MetaLensSpec(
    field.key,
    { json.convert<MoshiNode, T>(it) },
    { json.asJsonObject(it) }
)

object IntegralNumberAdapter : JsonAdapter.Factory {
    override fun create(type: Type, annotations: Set<Annotation>, moshi: Moshi) = when (type) {
        Any::class.java -> object : JsonAdapter<Any>() {
            private val delegate = moshi.nextAdapter<Any>(IntegralNumberAdapter, type, annotations)

            override fun fromJson(reader: JsonReader) = delegate.fromJson(reader)?.integral()

            override fun toJson(writer: JsonWriter, value: Any?) = delegate.toJson(writer, value)
        }

        else -> null
    }

    private fun Any.integral(): Any = when (this) {
        is Double if this % 1.0 == 0.0 && this in MIN_SAFE..MAX_SAFE -> toLong()
        is List<*> -> map { it?.integral() }
        is Map<*, *> -> mapValues { (_, v) -> v?.integral() }
        else -> this
    }

    private const val MAX_SAFE = 9007199254740992.0
    private const val MIN_SAFE = -9007199254740992.0
}

object MetaAdapter : JsonAdapter.Factory {
    override fun create(type: Type, annotations: Set<Annotation>, moshi: Moshi) =
        with(getRawType(type)) {
            when {
                isA(Meta::class.java) -> object : JsonAdapter<Meta>() {
                    override fun fromJson(reader: JsonReader): Meta {
                        val value = MoshiNode.wrap(reader.readJsonValue())
                        return Meta(value as? MoshiObject ?: MoshiObject())
                    }

                    override fun toJson(writer: JsonWriter, value: Meta?) {
                        val obj = value?.node ?: MoshiObject()
                        writer.jsonValue(obj.unwrap())
                    }
                }

                else -> null
            }
        }

    private fun Class<*>?.isA(testCase: Class<*>): Boolean =
        this?.let { testCase.isAssignableFrom(this) } ?: false
}

@KotshiJsonAdapterFactory
object McpJsonFactory : JsonAdapter.Factory by KotshiMcpJsonFactory

fun <T> AutoMappingConfiguration<T>.withMcpMappings() = apply {
    withStandardMappings()
    withAiMappings()
    value(Base64Blob)
    value(Domain)
    value(McpEntity)
    value(McpRpcMethod)
    value(McpMessageId)
    value(Priority)
    value(ProtocolVersion)
    value(PromptName)
    value(ResourceName)
    value(ResourceUriTemplate)
    value(Size)
    value(TaskId)
    value(TimeToLive)
    value(TtlMs)
    value(ToolUseId)
    value(Version)
}
