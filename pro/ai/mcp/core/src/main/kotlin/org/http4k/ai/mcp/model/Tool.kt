package org.http4k.ai.mcp.model

import dev.forkhandles.values.Value
import dev.forkhandles.values.ValueFactory
import org.http4k.ai.model.ToolName
import org.http4k.core.Uri
import org.http4k.lens.BiDiMapping
import org.http4k.lens.ParamMeta
import org.http4k.lens.ParamMeta.BooleanParam
import org.http4k.lens.ParamMeta.IntegerParam
import org.http4k.lens.ParamMeta.NumberParam
import org.http4k.lens.ParamMeta.ObjectParam
import org.http4k.lens.ParamMeta.StringParam
import org.http4k.lens.StringBiDiMappings
import org.http4k.lens.StringBiDiMappings.nonBlank
import org.http4k.lens.StringBiDiMappings.nonEmpty
import org.http4k.ai.mcp.ToolRequest
import org.http4k.ai.mcp.ToolResponse
import org.http4k.ai.mcp.model.ToolArgLensSpec.Companion.mapWithNewMeta
import org.http4k.ai.mcp.util.McpNodeType
import java.time.Instant
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeFormatter.ISO_LOCAL_DATE
import java.time.format.DateTimeFormatter.ISO_LOCAL_DATE_TIME
import java.time.format.DateTimeFormatter.ISO_LOCAL_TIME
import java.time.format.DateTimeFormatter.ISO_OFFSET_DATE_TIME
import java.time.format.DateTimeFormatter.ISO_OFFSET_TIME
import java.time.format.DateTimeFormatter.ISO_ZONED_DATE_TIME

/**
 * Description of a Tool capability.
 */
class Tool private constructor(
    val name: ToolName,
    val description: String,
    val args: List<McpCapabilityLens<ToolRequest, *>>,
    val output: McpCapabilityLens<ToolResponse.Ok, *>? = null,
    val title: String? = null,
    val annotations: ToolAnnotations? = null,
    val icons: List<Icon>? = null,
    val execution: ToolExecution? = null,
    val meta: Meta? = null,
) : CapabilitySpec {
    constructor(
        name: String,
        description: String,
        vararg arguments: McpCapabilityLens<ToolRequest, *>,
        output: McpCapabilityLens<ToolResponse.Ok, *>? = null,
        title: String? = null,
        annotations: ToolAnnotations? = null,
        icons: List<Icon>? = null,
        execution: ToolExecution? = null,
        meta: Meta? = null
    ) : this(ToolName.of(name), description, arguments.toList(), output, title, annotations, icons, execution, meta)

    /**
     * A typesafe tool argument lens. Use the extension functions below to create a lens for a specific type.
     */
    data object Arg

    /**
     * A typesafe tool response lens. Use the extension functions below to create a lens for a specific type.
     */
    data object Output

    class ArgList<T>(val delegate: List<T>) : List<T> by delegate
}


/**
 * Typesafe extension functions for creating tool argument lenses.
 */
fun Tool.Arg.string() = mapWithNewMeta({ it.toString() }, { it }, StringParam)
fun Tool.Arg.nonEmptyString() = string().map(nonEmpty())
fun Tool.Arg.nonBlankString() = string().map(nonBlank())
fun Tool.Arg.boolean() = mapWithNewMeta({ it as Boolean }, { it }, BooleanParam)
fun Tool.Arg.double() = mapWithNewMeta({ it as Double }, { it }, NumberParam)
fun Tool.Arg.float() = mapWithNewMeta({ it as Float }, { it }, NumberParam)
fun Tool.Arg.int() = mapWithNewMeta({ it as Int }, { it }, IntegerParam)
fun Tool.Arg.value() = mapWithNewMeta({ it as Int }, { it }, IntegerParam)
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

@Suppress("UNCHECKED_CAST")
inline fun <reified VALUE : Value<T>, reified T : Any> Tool.Arg.value(vf: ValueFactory<VALUE, T>) =
    mapWithNewMeta({ vf.of(it as T) }, { it.value }, paramMeta<T>())

fun <NEXT, OUT> ToolArgLensSpec<OUT>.map(mapping: BiDiMapping<OUT, NEXT>) = map(mapping::invoke, mapping::invoke)

inline fun <reified T : Any> paramMeta() = when (T::class) {
    String::class -> StringParam
    Int::class -> IntegerParam
    Long::class -> IntegerParam
    Boolean::class -> BooleanParam
    Double::class -> NumberParam
    Float::class -> NumberParam
    LocalDate::class -> StringParam
    Instant::class -> StringParam
    Uri::class -> StringParam
    else -> ObjectParam
}
