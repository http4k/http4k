package org.http4k.lens

import org.http4k.lens.ParamMeta.BooleanParam
import org.http4k.lens.ParamMeta.NumberParam
import org.http4k.lens.ParamMeta.StringParam

typealias PathLens<T> = Lens<String, T>

open class PathSpec<MID, OUT>(internal val delegate: LensSpec<String, String, OUT>) {
    open fun of(name: String, description: String? = null): PathLens<OUT> {
        val getLens = delegate.get(name)
        return object : Lens<String, OUT>(Meta(true, "path", StringParam, name, description), { getLens(it).firstOrNull() ?: throw LensFailure() }) {
            override fun toString(): String = "{$name}"
        }
    }

    fun <NEXT> map(nextIn: (OUT) -> NEXT): PathSpec<MID, NEXT> = PathSpec(delegate.map(nextIn))

    internal fun <NEXT> mapWithNewMeta(nextIn: (OUT) -> NEXT, paramMeta: ParamMeta): PathSpec<MID, NEXT> = PathSpec(delegate.mapWithNewMeta(nextIn, paramMeta))

}

object Path : PathSpec<String, String>(LensSpec<String, String, String>("path", StringParam,
    LensGet { _, target -> listOf(target) })) {

    fun fixed(name: String): PathLens<String> {
        val getLens = delegate.get(name)
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
fun Path.regex(pattern: String, group: Int = 1): PathSpec<String, String> {
    val regex = pattern.toRegex()
    return this.map { regex.matchEntire(it)?.groupValues?.get(group)!! }
}
