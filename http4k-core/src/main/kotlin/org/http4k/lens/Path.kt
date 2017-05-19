package org.http4k.lens

import org.http4k.lens.ParamMeta.BooleanParam
import org.http4k.lens.ParamMeta.NumberParam
import org.http4k.lens.ParamMeta.StringParam

typealias PathLens<T> = Lens<String, T>

/**
 * Represents a uni-directional extraction of an entity from a target path segment.
 */
open class PathSpec<out OUT>(protected val paramMeta: ParamMeta, internal val get: LensGet<String, String, OUT>) {
    open fun of(name: String, description: String? = null): PathLens<OUT> {
        val getLens = get(name)
        return object : Lens<String, OUT>(Meta(true, "path", paramMeta, name, description), { getLens(it).firstOrNull() ?: throw LensFailure() }) {
            override fun toString(): String = "{$name}"
        }
    }

    /**
     * Create another PathSpec which applies the uni-directional transformation to the result. Any resultant Lens can only be
     * used to extract the final type from a target.
     */
    fun <NEXT> map(nextIn: (OUT) -> NEXT): PathSpec<NEXT> = PathSpec(paramMeta, get.map(nextIn))

    internal fun <NEXT> mapWithNewMeta(nextIn: (OUT) -> NEXT, newMeta: ParamMeta): PathSpec<NEXT> = PathSpec(newMeta, get.map(nextIn))

}

object Path : PathSpec<String>(StringParam, LensGet { _, target -> listOf(target) }) {

    fun fixed(name: String): PathLens<String> {
        val getLens = get(name)
        return object : Lens<String, String>(Meta(true, "path", StringParam, name),
            { getLens(it).let { if (it == listOf(name)) name else throw LensFailure() } }) {
            override fun toString(): String = name
            override fun iterator(): Iterator<Meta> = emptyList<Meta>().iterator()
        }
    }
}

fun Path.int() = Path.mapWithNewMeta(String::toInt, NumberParam)
fun Path.long() = Path.mapWithNewMeta(String::toLong, NumberParam)
fun Path.double() = Path.mapWithNewMeta(String::toDouble, NumberParam)
fun Path.float() = Path.mapWithNewMeta(String::toFloat, NumberParam)
fun Path.boolean() = Path.mapWithNewMeta(::safeBooleanFrom, BooleanParam)
fun Path.localDate() = Path.map(java.time.LocalDate::parse)
fun Path.dateTime() = Path.map(java.time.LocalDateTime::parse)
fun Path.zonedDateTime() = Path.map(java.time.ZonedDateTime::parse)
fun Path.uuid() = Path.map(java.util.UUID::fromString)
fun Path.regex(pattern: String, group: Int = 1): PathSpec<String> {
    val regex = pattern.toRegex()
    return this.map { regex.matchEntire(it)?.groupValues?.get(group)!! }
}
