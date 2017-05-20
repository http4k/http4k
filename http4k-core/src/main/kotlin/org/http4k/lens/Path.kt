package org.http4k.lens

import org.http4k.core.Request
import org.http4k.core.decode
import org.http4k.core.encode
import org.http4k.lens.ParamMeta.BooleanParam
import org.http4k.lens.ParamMeta.NumberParam
import org.http4k.lens.ParamMeta.StringParam
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.*

typealias PathLens<T> = Lens<String, T>

class BiDiPathLens<FINAL>(meta: Meta, get: (String) -> FINAL, private val set: (FINAL, Request) -> Request) : Lens<String, FINAL>(meta, get) {
    override fun toString(): String = "{${meta.name}}"

    /**
     * Lens operation to set the value into the target url
     */
    @Suppress("UNCHECKED_CAST")
    operator fun <R : Request> invoke(value: FINAL, target: R): R = set(value, target) as R

    /**
     * Bind this Lens to a value, so we can set it into a target
     */
    infix fun <R : Request> of(value: FINAL): (R) -> R = { invoke(value, it) }
}

/**
 * Represents a uni-directional extraction of an entity from a target path segment.
 */
open class PathLensSpec<out OUT>(protected val paramMeta: ParamMeta, internal val get: LensGet<String, String, OUT>) {
    open fun of(name: String, description: String? = null): PathLens<OUT> {
        val getLens = get(name)
        val meta = Meta(true, "path", paramMeta, name, description)
        return object : Lens<String, OUT>(meta, { getLens(it).firstOrNull() ?: throw LensFailure() }) {
            override fun toString(): String = "{$name}"
        }
    }

    /**
     * Create another PathLensSpec which applies the uni-directional transformation to the result. Any resultant Lens can only be
     * used to extract the final type from a target path segment.
     */
    fun <NEXT> map(nextIn: (OUT) -> NEXT): PathLensSpec<NEXT> = PathLensSpec(paramMeta, get.map(nextIn))
}

open class BiDiPathLensSpec<OUT>(paramMeta: ParamMeta,
                                 get: LensGet<String, String, OUT>,
                                 private val set: LensSet<Request, String, OUT>) : PathLensSpec<OUT>(paramMeta, get) {

    /**
     * Create another BiDiPathLensSpec which applies the bi-directional transformations to the result. Any resultant Lens can be
     * used to extract or insert the final type from/into a path segment.
     */
    fun <NEXT> map(nextIn: (OUT) -> NEXT, nextOut: (NEXT) -> OUT) = BiDiPathLensSpec(paramMeta, get.map(nextIn), set.map(nextOut))

    internal fun <NEXT> mapWithNewMeta(nextIn: (OUT) -> NEXT, nextOut: (NEXT) -> OUT, newMeta: ParamMeta): BiDiPathLensSpec<NEXT> = BiDiPathLensSpec(newMeta, get.map(nextIn), set.map(nextOut))

    /**
     * Create a lens for this Spec
     */
    override fun of(name: String, description: String?): BiDiPathLens<OUT> {
        val meta = Meta(true, "path", paramMeta, name, description)
        val getLens = get(name)
        val setLens = set(name)

        return BiDiPathLens(meta,
            { getLens(it).firstOrNull() ?: throw LensFailure() },
            { it: OUT, target: Request -> setLens(listOf(it), target) })
    }
}

object Path : BiDiPathLensSpec<String>(StringParam,
    LensGet { _, target -> listOf(target.decode()) },
    LensSet { name, values, target -> target.uri(target.uri.path(target.uri.path.replaceFirst("{$name}", values.first().encode()))) }) {

    fun fixed(name: String): PathLens<String> {
        val getLens = get(name)
        return object : Lens<String, String>(Meta(true, "path", StringParam, name),
            { getLens(it).let { if (it == listOf(name)) name else throw LensFailure() } }) {
            override fun toString(): String = name
            override fun iterator(): Iterator<Meta> = emptyList<Meta>().iterator()
        }
    }
}

fun Path.string() = this
fun Path.int() = string().mapWithNewMeta(String::toInt, Int::toString, NumberParam)
fun Path.long() = string().mapWithNewMeta(String::toLong, Long::toString, NumberParam)
fun Path.double() = string().mapWithNewMeta(String::toDouble, Double::toString, NumberParam)
fun Path.float() = string().mapWithNewMeta(String::toFloat, Float::toString, NumberParam)
fun Path.boolean() = string().mapWithNewMeta(::safeBooleanFrom, Boolean::toString, BooleanParam)
fun Path.localDate() = string().map(LocalDate::parse, DateTimeFormatter.ISO_LOCAL_DATE::format)
fun Path.dateTime() = string().map(LocalDateTime::parse, DateTimeFormatter.ISO_LOCAL_DATE_TIME::format)
fun Path.zonedDateTime() = string().map(ZonedDateTime::parse, DateTimeFormatter.ISO_ZONED_DATE_TIME::format)
fun Path.uuid() = string().map(UUID::fromString, java.util.UUID::toString)
fun Path.regex(pattern: String, group: Int = 1): PathLensSpec<String> {
    val toRegex = pattern.toRegex()
    return string().map { toRegex.matchEntire(it)?.groupValues?.get(group)!! }
}
