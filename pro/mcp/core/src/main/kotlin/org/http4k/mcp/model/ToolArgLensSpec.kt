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
    private val convert: (Meta) -> McpNodeType
) : BiDiLensBuilder<ToolRequest, OUT> {

    fun <NEXT> map(nextIn: (OUT) -> NEXT, nextOut: (NEXT) -> OUT) = mapWithNewMeta(nextIn, nextOut, paramMeta)

    fun <NEXT> mapWithNewMeta(nextIn: (OUT) -> NEXT, nextOut: (NEXT) -> OUT, paramMeta: ParamMeta) =
        ToolArgLensSpec(paramMeta, get.map(nextIn), set.map(nextOut), convert)

    fun <NEXT> mapWithNew(newConvert: (Meta) -> McpNodeType) =
        ToolArgLensSpec(paramMeta, get, set, newConvert)

    override fun optional(name: String, description: String?, metadata: Map<String, Any>): ToolArgLens<OUT?> {
        val meta = Meta(false, "toolRequest", paramMeta, name, description, emptyMap())
        val getLens = get(name)
        val setLens = set(name)
        return ToolArgLens(
            meta, { getLens(it).firstOrNull() },
            { out: OUT?, target: ToolRequest -> setLens(out?.let { listOf(it) } ?: emptyList(), target) },
            convert
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
        val meta = Meta(false, "toolRequest", paramMeta, name, description, emptyMap())
        val getLens = get(name)
        val setLens = set(name)
        return ToolArgLens(
            meta, { getLens(it).run { if (isEmpty()) default(it) else first() } },
            { out: OUT, target: ToolRequest -> setLens(out?.let { listOf(it) } ?: emptyList(), target) },
            convert
        )
    }

    override fun required(name: String, description: String?, metadata: Map<String, Any>): ToolArgLens<OUT> {
        val meta = Meta(true, "toolRequest", paramMeta, name, description, emptyMap())
        val getLens = get(name)
        val setLens = set(name)
        return ToolArgLens(
            meta, { getLens(it).firstOrNull() ?: throw LensFailure(listOf(Missing(meta)), target = it) },
            { out: OUT?, target: ToolRequest -> setLens(out?.let(::listOf) ?: emptyList(), target) },
            convert
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
                convert
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
                convert
            )
        }

        override fun required(name: String, description: String?, metadata: Map<String, Any>): ToolArgLens<List<OUT>> {
            val getLens = get(name)
            val setLens = set(name)
            val meta = Meta(true, "toolRequest", ArrayParam(paramMeta), name, description, metadata)
            return ToolArgLens(
                meta, { getLens(it).run { ifEmpty { throw LensFailure(Missing(meta), target = it) } } },
                { out: List<OUT>, target -> setLens(ArgList(out), target) },
                convert
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
        }, { it.paramMeta.toSchema(it.description) })
}

fun <NEXT, OUT> ToolArgLensSpec<OUT>.map(mapping: BiDiMapping<OUT, NEXT>) =
    map(mapping::invoke, mapping::invoke)

fun Tool.Arg.string() = mapWithNewMeta({ it.toString() }, { it }, StringParam)
fun Tool.Arg.nonEmptyString() = string().map(nonEmpty())
fun Tool.Arg.nonBlankString() = string().map(nonBlank())
fun Tool.Arg.boolean() = mapWithNewMeta({ it as Boolean }, { it }, BooleanParam)
fun Tool.Arg.double() = mapWithNewMeta({ it as Double }, { it }, NumberParam)
fun Tool.Arg.float() = mapWithNewMeta({ it as Float }, { it }, NumberParam)
fun Tool.Arg.int() = mapWithNewMeta({ it as Int }, { it }, IntegerParam)
fun Tool.Arg.value() = mapWithNewMeta({ it as Int }, { it }, IntegerParam)

@Suppress("UNCHECKED_CAST")
inline fun <reified VALUE : Value<T>, reified T : Any> Tool.Arg.value(vf: ValueFactory<VALUE, T>) =
    mapWithNewMeta(
        { vf.of(it as T) }, { it.value }, when (T::class) {
            String::class -> StringParam
            Int::class -> IntegerParam
            Long::class -> IntegerParam
            Boolean::class -> BooleanParam
            Double::class -> NumberParam
            Float::class -> NumberParam
            else -> ObjectParam
        }
    )

fun Tool.Arg.long() = mapWithNewMeta({ it as Long }, { it }, IntegerParam)
fun Tool.Arg.uuid() = string().map(StringBiDiMappings.uuid())
fun Tool.Arg.uri() = string().map(StringBiDiMappings.uri())
fun Tool.Arg.urlEncoded() = string().map(StringBiDiMappings.urlEncoded())
fun Tool.Arg.regexObject() = string().map(StringBiDiMappings.regexObject())
fun Tool.Arg.period() = string().map(StringBiDiMappings.period())
fun Tool.Arg.duration() = string().map(StringBiDiMappings.duration())
fun Tool.Arg.base64() = string().map(StringBiDiMappings.base64())
fun Tool.Arg.instant() = string().map(StringBiDiMappings.instant())
fun Tool.Arg.yearMonth() = string().map(StringBiDiMappings.yearMonth())
fun Tool.Arg.dateTime(formatter: DateTimeFormatter = ISO_LOCAL_DATE_TIME) =
    string().map(StringBiDiMappings.localDateTime(formatter))

fun Tool.Arg.zonedDateTime(formatter: DateTimeFormatter = ISO_ZONED_DATE_TIME) =
    string().map(StringBiDiMappings.zonedDateTime(formatter))

fun Tool.Arg.localDate(formatter: DateTimeFormatter = ISO_LOCAL_DATE) =
    string().map(StringBiDiMappings.localDate(formatter))

fun Tool.Arg.localTime(formatter: DateTimeFormatter = ISO_LOCAL_TIME) =
    string().map(StringBiDiMappings.localTime(formatter))

fun Tool.Arg.offsetTime(formatter: DateTimeFormatter = ISO_OFFSET_TIME) =
    string().map(StringBiDiMappings.offsetTime(formatter))

fun Tool.Arg.offsetDateTime(formatter: DateTimeFormatter = ISO_OFFSET_DATE_TIME) =
    string().map(StringBiDiMappings.offsetDateTime(formatter))

fun Tool.Arg.zoneId() = string().map(StringBiDiMappings.zoneId())
fun Tool.Arg.zoneOffset() = string().map(StringBiDiMappings.zoneOffset())
fun Tool.Arg.locale() = string().map(StringBiDiMappings.locale())

inline fun <reified T : Enum<T>> Tool.Arg.enum() =
    mapWithNewMeta({ enumValueOf<T>(it.toString()) }, { it }, ParamMeta.EnumParam(T::class))

