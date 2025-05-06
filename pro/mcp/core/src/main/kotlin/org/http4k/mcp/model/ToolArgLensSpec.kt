package org.http4k.mcp.model

import dev.forkhandles.values.Value
import dev.forkhandles.values.ValueFactory
import org.http4k.lens.BiDiLensBuilder
import org.http4k.lens.BiDiMapping
import org.http4k.lens.BiDiMultiLensSpec
import org.http4k.lens.Lens
import org.http4k.lens.LensExtractor
import org.http4k.lens.LensFailure
import org.http4k.lens.LensGet
import org.http4k.lens.LensSet
import org.http4k.lens.Meta
import org.http4k.lens.Missing
import org.http4k.lens.ParamMeta
import org.http4k.lens.ParamMeta.ArrayParam
import org.http4k.lens.ParamMeta.BooleanParam
import org.http4k.lens.ParamMeta.IntegerParam
import org.http4k.lens.ParamMeta.NumberParam
import org.http4k.lens.ParamMeta.ObjectParam
import org.http4k.lens.ParamMeta.StringParam
import org.http4k.lens.StringBiDiMappings
import org.http4k.lens.StringBiDiMappings.nonBlank
import org.http4k.lens.StringBiDiMappings.nonEmpty
import org.http4k.mcp.ToolRequest
import org.http4k.mcp.model.Tool.ArgList
import org.http4k.mcp.model.ToolArgLensSpec.Companion.mapWithNewMeta
import org.http4k.mcp.util.McpNodeType
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeFormatter.ISO_LOCAL_DATE
import java.time.format.DateTimeFormatter.ISO_LOCAL_DATE_TIME
import java.time.format.DateTimeFormatter.ISO_LOCAL_TIME
import java.time.format.DateTimeFormatter.ISO_OFFSET_DATE_TIME
import java.time.format.DateTimeFormatter.ISO_OFFSET_TIME
import java.time.format.DateTimeFormatter.ISO_ZONED_DATE_TIME


open class ToolArgLensSpec<OUT>(
    val paramMeta: ParamMeta,
    internal val get: LensGet<ToolRequest, OUT>,
    internal val set: LensSet<ToolRequest, OUT>,
    private val toSchema: ToolArgLens<*>.() -> McpNodeType
) : BiDiLensBuilder<ToolRequest, OUT> {

    fun <NEXT> map(nextIn: (OUT) -> NEXT, nextOut: (NEXT) -> OUT) = mapWithNewMeta(nextIn, nextOut, paramMeta)

    fun <NEXT> mapWithNewMeta(nextIn: (OUT) -> NEXT, nextOut: (NEXT) -> OUT, paramMeta: ParamMeta) =
        ToolArgLensSpec(paramMeta, get.map(nextIn), set.map(nextOut), toSchema)

    fun <NEXT> mapWithNew(toSchema: ToolArgLens<*>.() -> McpNodeType) =
        ToolArgLensSpec(paramMeta, get, set, toSchema)

    override fun optional(name: String, description: String?, metadata: Map<String, Any>): ToolArgLens<OUT?> {
        val meta = Meta(false, "toolRequest", paramMeta, name, description, metadata)
        val getLens = get(name)
        val setLens = set(name)
        return ToolArgLens(
            meta, { getLens(it).firstOrNull() },
            { out: OUT?, target: ToolRequest -> setLens(out?.let { listOf(it) } ?: emptyList(), target) },
            toSchema
        )
    }

    override fun defaulted(
        name: String,
        default: OUT,
        description: String?,
        metadata: Map<String, Any>
    ) = defaulted(name, { default }, description, metadata)

    override fun defaulted(
        name: String, default: LensExtractor<ToolRequest, OUT>,
        description: String?, metadata: Map<String, Any>
    ): ToolArgLens<OUT> {
        val meta = Meta(false, "toolRequest", paramMeta, name, description, metadata)
        val getLens = get(name)
        val setLens = set(name)
        return ToolArgLens(
            meta, { getLens(it).run { if (isEmpty()) default(it) else first() } },
            { out: OUT, target: ToolRequest -> setLens(out?.let { listOf(it) } ?: emptyList(), target) },
            toSchema
        )
    }

    override fun required(name: String, description: String?, metadata: Map<String, Any>): ToolArgLens<OUT> {
        val meta = Meta(true, "toolRequest", paramMeta, name, description, metadata)
        val getLens = get(name)
        val setLens = set(name)
        return ToolArgLens(
            meta, { getLens(it).firstOrNull() ?: throw LensFailure(listOf(Missing(meta)), target = it) },
            { out: OUT?, target: ToolRequest -> setLens(out?.let(::listOf) ?: emptyList(), target) },
            toSchema
        )
    }

    val multi = ToolRequestOUTBiDiMultiLensSpec()

    inner class ToolRequestOUTBiDiMultiLensSpec : BiDiMultiLensSpec<ToolRequest, OUT> {
        override fun defaulted(
            name: String,
            default: List<OUT>,
            description: String?,
            metadata: Map<String, Any>
        ): ToolArgLens<List<OUT>> =
            defaulted(
                name,
                Lens(Meta(false, "toolRequest", ArrayParam(paramMeta), name, description, metadata)) { default },
                description
            )

        override fun defaulted(
            name: String,
            default: LensExtractor<ToolRequest, List<OUT>>,
            description: String?,
            metadata: Map<String, Any>
        ): ToolArgLens<List<OUT>> {
            val getLens = get(name)
            val setLens = set(name)
            return ToolArgLens(
                Meta(false, "toolRequest", ArrayParam(paramMeta), name, description, metadata),
                { getLens(it).run { ifEmpty { default(it) } } },
                { out: List<OUT>, target: ToolRequest -> setLens(ArgList(out), target) },
                toSchema
            )
        }

        override fun optional(
            name: String,
            description: String?,
            metadata: Map<String, Any>
        ): ToolArgLens<List<OUT>?> {
            val getLens = get(name)
            val setLens = set(name)
            return ToolArgLens(
                Meta(false, "toolRequest", ArrayParam(paramMeta), name, description, metadata),
                { getLens(it).run { ifEmpty { null } } },
                { out: List<OUT>?, target: ToolRequest -> setLens(ArgList(out ?: emptyList()), target) },
                toSchema
            )
        }

        override fun required(name: String, description: String?, metadata: Map<String, Any>): ToolArgLens<List<OUT>> {
            val getLens = get(name)
            val setLens = set(name)
            val meta = Meta(true, "toolRequest", ArrayParam(paramMeta), name, description, metadata)
            return ToolArgLens(
                meta, { getLens(it).run { ifEmpty { throw LensFailure(Missing(meta), target = it) } } },
                { out: List<OUT>, target -> setLens(ArgList(out), target) },
                toSchema
            )
        }
    }

    companion object : ToolArgLensSpec<Any>(
        ObjectParam,
        LensGet { name, target ->
            @Suppress("UNCHECKED_CAST")
            when (val value = target[name]) {
                null -> emptyList()
                is List<*> -> value as List<Any>
                else -> listOf(value)
            }
        },
        LensSet { name: String, values: List<Any>, target: ToolRequest ->
            when (values) {
                is ArgList -> target.copy(args = target.args + (name to values.delegate))
                else -> values.fold(target) { m, v -> m.copy(args = m.args + (name to v)) }
            }
        }, { meta.toSchema() })
}
