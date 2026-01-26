package org.http4k.ai.mcp.model

import dev.forkhandles.values.Value
import dev.forkhandles.values.ValueFactory
import org.http4k.ai.mcp.model.ElicitationLensSpec.Companion.mapWithNewMeta
import org.http4k.ai.mcp.util.McpJson
import org.http4k.ai.mcp.util.McpJson.bool
import org.http4k.ai.mcp.util.McpJson.decimal
import org.http4k.ai.mcp.util.McpJson.integer
import org.http4k.ai.mcp.util.McpJson.string
import org.http4k.ai.mcp.util.McpJson.text
import org.http4k.core.Uri
import org.http4k.format.MoshiBoolean
import org.http4k.format.MoshiDecimal
import org.http4k.format.MoshiInteger
import org.http4k.format.MoshiNode
import org.http4k.format.MoshiString
import org.http4k.lens.BiDiMapping
import org.http4k.lens.ParamMeta.BooleanParam
import org.http4k.lens.ParamMeta.EnumParam
import org.http4k.lens.ParamMeta.IntegerParam
import org.http4k.lens.ParamMeta.NumberParam
import org.http4k.lens.ParamMeta.StringParam
import org.http4k.lens.StringBiDiMappings
import org.http4k.lens.StringBiDiMappings.nonBlank
import org.http4k.lens.StringBiDiMappings.nonEmpty
import java.time.Instant
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeFormatter.ISO_LOCAL_DATE
import java.time.format.DateTimeFormatter.ISO_LOCAL_DATE_TIME
import java.time.format.DateTimeFormatter.ISO_LOCAL_TIME
import java.time.format.DateTimeFormatter.ISO_OFFSET_DATE_TIME
import java.time.format.DateTimeFormatter.ISO_OFFSET_TIME
import java.time.format.DateTimeFormatter.ISO_ZONED_DATE_TIME

object Elicitation {

    /**
     * Metadata for elicitation parameters which can be added to the schema of the elicitation
     */
    sealed class Metadata<IN, T : Any>(val name: String) {
        abstract val value: T

        internal open fun data() = listOf(name to value)

        override fun toString(): String = "$name: $value"

        abstract class string<T : Any>(name: String) : Metadata<String, T>(name) {
            data class MinLength(override val value: Int) : string<Int>("minLength")
            data class MaxLength(override val value: Int) : string<Int>("maxLength")
            data class Pattern(override val value: Regex) : string<Regex>("pattern")

            sealed class Format(override val value: String) : string<String>("format") {
                data object Email : Format("email")
                data object Url : Format("url")
                data object Date : Format("date")
                data object DateTime : Format("date-time")
                data class Custom(override val value: String) : Format(value)
            }
        }

        abstract class number<T : Any>(name: String) : Metadata<Number, T>(name) {
            data class Min(override val value: Number) : number<Number>("minimum")
            data class Max(override val value: Number) : number<Number>("maximum")
        }

        abstract class integer<T : Any>(name: String) : Metadata<Long, T>(name) {
            data class Min(override val value: Int) : integer<Int>("minimum")
            data class Max(override val value: Int) : integer<Int>("maximum")
        }

        abstract class boolean<T : Any>(name: String) : Metadata<Boolean, T>(name) {
            data class Default(override val value: Boolean) : boolean<Boolean>("default")
        }

        class EnumNames<T : Enum<T>>(mappings: Map<T, String>) :
            Metadata<T, List<MoshiNode>>("enum") {

            private val sorted = mappings.toList().sortedBy { it.second }
            override val value = mappings.keys.sortedBy { it.ordinal }.map { McpJson.string(it.name) }

            override fun data() = listOf(
                "oneOf" to sorted.map {
                    McpJson.obj(
                        "title" to McpJson.string(it.second),
                        "const" to McpJson.string(it.first.name)
                    )
                }
            )

            companion object {
                /**
                 * Create a set of names from an enum class.
                 */
                inline operator fun <reified T : Enum<T>> invoke() =
                    EnumNames(enumValues<T>().associateWith { it.toString() })
            }
        }
    }
}

fun Elicitation.string() = mapWithNewMeta(
    { text(it) },
    { string(it) },
    StringParam
)

fun Elicitation.int() = mapWithNewMeta(
    { integer(it) },
    { MoshiInteger(it.toInt()) },
    IntegerParam
)

fun Elicitation.number() = mapWithNewMeta(
    { decimal(it) },
    { MoshiDecimal(it.toDouble()) },
    NumberParam
)

fun Elicitation.boolean() = mapWithNewMeta({ bool(it) }, { MoshiBoolean(it) }, BooleanParam)

inline fun <reified T : Enum<T>> Elicitation.enum() =
    mapWithNewMeta(
        { enumValueOf<T>(it.toString()) },
        { MoshiString(it.name) },
        EnumParam(T::class)
    )

/**
 * Typesafe extension functions for creating Elicitation lenses.
 */
fun Elicitation.nonEmptyString() = string().map(nonEmpty())
fun Elicitation.nonBlankString() = string().map(nonBlank())
fun Elicitation.uuid() = string().map(StringBiDiMappings.uuid())
fun Elicitation.uri() = string().map(StringBiDiMappings.uri(), mapOf("format" to string("uri")))
fun Elicitation.urlEncoded() = string().map(StringBiDiMappings.urlEncoded())
fun Elicitation.regexObject() = string().map(StringBiDiMappings.regexObject())
fun Elicitation.period() = string().map(StringBiDiMappings.period())
fun Elicitation.duration() = string().map(StringBiDiMappings.duration())
fun Elicitation.base64() = string().map(StringBiDiMappings.base64())
fun Elicitation.instant() = string().map(StringBiDiMappings.instant(), mapOf("format" to string("date-time")))
fun Elicitation.yearMonth() = string().map(StringBiDiMappings.yearMonth())
fun Elicitation.dateTime(formatter: DateTimeFormatter = ISO_LOCAL_DATE_TIME) =
    string().map(StringBiDiMappings.localDateTime(formatter))

fun Elicitation.zonedDateTime(formatter: DateTimeFormatter = ISO_ZONED_DATE_TIME) =
    string().map(StringBiDiMappings.zonedDateTime(formatter))

fun Elicitation.localDate(formatter: DateTimeFormatter = ISO_LOCAL_DATE) =
    string().map(StringBiDiMappings.localDate(formatter), mapOf("format" to string("date")))

fun Elicitation.localTime(formatter: DateTimeFormatter = ISO_LOCAL_TIME) =
    string().map(StringBiDiMappings.localTime(formatter))

fun Elicitation.offsetTime(formatter: DateTimeFormatter = ISO_OFFSET_TIME) =
    string().map(StringBiDiMappings.offsetTime(formatter))

fun Elicitation.offsetDateTime(formatter: DateTimeFormatter = ISO_OFFSET_DATE_TIME) =
    string().map(StringBiDiMappings.offsetDateTime(formatter))

fun Elicitation.zoneId() = string().map(StringBiDiMappings.zoneId())
fun Elicitation.zoneOffset() = string().map(StringBiDiMappings.zoneOffset())
fun Elicitation.locale() = string().map(StringBiDiMappings.locale())

inline fun <reified VALUE : Value<T>, reified T : Any> Elicitation.value(vf: ValueFactory<VALUE, T>) =
    mapWithNewMeta(
        { vf.of(it as T) }, { McpJson.asJsonObject(it.value) }, paramMeta<T>(),
        listOfNotNull(
            T::class.takeIf { it == Uri::class }?.let { "format" to string("uri") },
            T::class.takeIf { it == LocalDate::class }?.let { "format" to string("date") },
            T::class.takeIf { it == Instant::class }?.let { "format" to string("date-time") }
        ).toMap()
    )

fun <NEXT, OUT> ElicitationLensSpec<OUT>.map(
    mapping: BiDiMapping<OUT, NEXT>,
    newMetadata: Map<String, MoshiNode> = emptyMap()
) = mapWithNewMeta(mapping::invoke, mapping::invoke, paramMeta, newMetadata)
