package org.http4k.ai.mcp.model

import org.http4k.lens.BiDiLensBuilder
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
import org.http4k.lens.ParamMeta.ObjectParam
import org.http4k.ai.mcp.ToolRequest
import org.http4k.ai.mcp.model.Tool.ArgList
import org.http4k.ai.mcp.util.McpNodeType

open class ToolArgLensSpec<OUT>(
    val paramMeta: ParamMeta,
    internal val get: LensGet<ToolRequest, OUT>,
    internal val set: LensSet<ToolRequest, OUT>,
    private val toSchema: McpCapabilityLens<ToolRequest, *>.(Map<String, Any>) -> McpNodeType
) : BiDiLensBuilder<ToolRequest, OUT> {

    fun <NEXT> map(nextIn: (OUT) -> NEXT, nextOut: (NEXT) -> OUT) = mapWithNewMeta(nextIn, nextOut, paramMeta)

    fun <NEXT> mapWithNewMeta(nextIn: (OUT) -> NEXT, nextOut: (NEXT) -> OUT, paramMeta: ParamMeta) =
        ToolArgLensSpec(paramMeta, get.map(nextIn), set.map(nextOut), toSchema)

    override fun optional(
        name: String,
        description: String?,
        metadata: Map<String, Any>
    ): McpCapabilityLens<ToolRequest, OUT?> {
        val meta = Meta(false, "toolRequest", paramMeta, name, description, metadata)
        val getLens = get(name)
        val setLens = set(name)
        return McpCapabilityLens(
            meta, { getLens(it).firstOrNull() },
            { out: OUT?, target -> setLens(out?.let { listOf(it) } ?: emptyList(), target) },
            { toSchema(it, metadata) }
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
    ): McpCapabilityLens<ToolRequest, OUT> {
        val meta = Meta(false, "toolRequest", paramMeta, name, description, metadata)
        val getLens = get(name)
        val setLens = set(name)
        return McpCapabilityLens(
            meta, { getLens(it).run { if (isEmpty()) default(it) else first() } },
            { out: OUT, target: ToolRequest -> setLens(out?.let { listOf(it) } ?: emptyList(), target) },
            { toSchema(it, metadata) }
        )
    }

    override fun required(
        name: String,
        description: String?,
        metadata: Map<String, Any>
    ): McpCapabilityLens<ToolRequest, OUT> {
        val meta = Meta(true, "toolRequest", paramMeta, name, description, metadata)
        val getLens = get(name)
        val setLens = set(name)
        return McpCapabilityLens(
            meta, { getLens(it).firstOrNull() ?: throw LensFailure(listOf(Missing(meta)), target = it) },
            { out: OUT?, target: ToolRequest -> setLens(out?.let(::listOf) ?: emptyList(), target) },
            { toSchema(it, metadata) }
        )
    }

    val multi = ToolRequestBiDiMultiLensSpec()

    inner class ToolRequestBiDiMultiLensSpec : BiDiMultiLensSpec<ToolRequest, OUT> {
        override fun defaulted(
            name: String,
            default: List<OUT>,
            description: String?,
            metadata: Map<String, Any>
        ): McpCapabilityLens<ToolRequest, List<OUT>> =
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
        ): McpCapabilityLens<ToolRequest, List<OUT>> {
            val getLens = get(name)
            val setLens = set(name)
            return McpCapabilityLens(
                Meta(false, "toolRequest", ArrayParam(paramMeta), name, description, metadata),
                { getLens(it).run { ifEmpty { default(it) } } },
                { out: List<OUT>, target: ToolRequest -> setLens(ArgList(out), target) },
                { toSchema(it, metadata) }
            )
        }

        override fun optional(
            name: String,
            description: String?,
            metadata: Map<String, Any>
        ): McpCapabilityLens<ToolRequest, List<OUT>?> {
            val getLens = get(name)
            val setLens = set(name)
            return McpCapabilityLens(
                Meta(false, "toolRequest", ArrayParam(paramMeta), name, description, metadata),
                { getLens(it).run { ifEmpty { null } } },
                { out: List<OUT>?, target: ToolRequest -> setLens(ArgList(out ?: emptyList()), target) },
                { toSchema(it, metadata) }
            )
        }

        override fun required(
            name: String,
            description: String?,
            metadata: Map<String, Any>
        ): McpCapabilityLens<ToolRequest, List<OUT>> {
            val getLens = get(name)
            val setLens = set(name)
            val meta = Meta(true, "toolRequest", ArrayParam(paramMeta), name, description, metadata)
            return McpCapabilityLens(
                meta, { getLens(it).run { ifEmpty { throw LensFailure(Missing(meta), target = it) } } },
                { out: List<OUT>, target -> setLens(ArgList(out), target) },
                { toSchema(it, metadata) }
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
        LensSet { name, values, target ->
            when (values) {
                is ArgList -> target.copy(args = target.args + (name to values.delegate))
                else -> values.fold(target) { m, v -> m.copy(args = m.args + (name to v)) }
            }
        }, { meta.toSchema() })
}

