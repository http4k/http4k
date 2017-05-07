package org.reekwest.http.lens

import org.reekwest.http.lens.ParamMeta.StringParam

typealias PathLens<T> = Lens<String, T>

open class PathSpec<MID, OUT>(internal val delegate: LensSpec<String, String, OUT>) {
    open fun of(name: String, description: String? = null): PathLens<OUT> {
        val getLens = delegate.get(name)
        return object : Lens<String, OUT>(Meta(true, "path", name, description), { getLens(it).firstOrNull() ?: throw LensFailure() }) {
            override fun toString(): String = "{$name}"
        }
    }

    fun <NEXT> map(nextIn: (OUT) -> NEXT): PathSpec<MID, NEXT> = PathSpec(delegate.map(nextIn))
}

object Path : PathSpec<String, String>(LensSpec<String, String, String>("path", StringParam,
    Get { _, target -> listOf(target) })) {

    fun fixed(name: String): PathLens<String> {
        val getLens = delegate.get(name)
        return object : Lens<String, String>(Meta(true, "path", name),
            { getLens(it).let { if (it == listOf(name)) name else throw LensFailure() } }) {
            override fun toString(): String = name
            override fun iterator(): Iterator<Meta> = emptyList<Meta>().iterator()
        }
    }
}

fun Path.int() = Path.map(String::toInt)
fun Path.long() = Path.map(String::toLong)
fun Path.double() = Path.map(String::toDouble)
fun Path.float() = Path.map(String::toFloat)
fun Path.boolean() = Path.map(::safeBooleanFrom)
fun Path.localDate() = Path.map(java.time.LocalDate::parse)
fun Path.dateTime() = Path.map(java.time.LocalDateTime::parse)
fun Path.zonedDateTime() = Path.map(java.time.ZonedDateTime::parse)
fun Path.uuid() = Path.map(java.util.UUID::fromString)

