package org.http4k.lens

import org.http4k.core.Request
import org.http4k.core.Uri
import org.http4k.lens.ParamMeta.BooleanParam
import org.http4k.lens.ParamMeta.NumberParam
import org.http4k.lens.ParamMeta.StringParam

typealias PathLens<T> = Lens<String, T>

class BiDiPathLens<FINAL>(meta: Meta, get: (String) -> FINAL, private val set: (FINAL) -> String) : Lens<String, FINAL>(meta, get) {

    /**
     * Lens operation to set the value into the target url
     */
    @Suppress("UNCHECKED_CAST")
    operator fun <R : Request> invoke(value: FINAL, target: R): R = target.uri(Uri.of(target.uri.path.replaceFirst("{${meta.name}}", set(value)))) as R

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

    internal fun <NEXT> mapWithNewMeta(nextIn: (OUT) -> NEXT, newMeta: ParamMeta): PathLensSpec<NEXT> = PathLensSpec(newMeta, get.map(nextIn))

}

open class BiDiPathLensSpec<OUT>(paramMeta: ParamMeta,
                                 get: LensGet<String, String, OUT>,
                                 private val set: LensSet<String, String, OUT>) : PathLensSpec<OUT>(paramMeta, get) {

    /**
     * Create another BiDiPathLensSpec which applies the bi-directional transformations to the result. Any resultant Lens can be
     * used to extract or insert the final type from/into a path segment.
     */
    fun <NEXT> map(nextIn: (OUT) -> NEXT, nextOut: (NEXT) -> OUT) = BiDiPathLensSpec(paramMeta, get.map(nextIn), set.map(nextOut))

    /**
     * Create a lens for this Spec
     */
    override fun of(name: String, description: String?): BiDiPathLens<OUT> {
        val meta = Meta(true, "path", paramMeta, name, description)
        val getLens = get(name)
        val setLens = set(name)

        return BiDiPathLens(meta,
            { getLens(it).firstOrNull() ?: throw LensFailure() },
            { it: OUT -> setLens(listOf(it), "") })
    }

}

object Path : BiDiPathLensSpec<String>(StringParam,
    LensGet { _, target -> listOf(target) },
    LensSet { _, values, _ -> values.first() }) {

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
fun Path.int() = Path.mapWithNewMeta(String::toInt, NumberParam)
fun Path.long() = Path.mapWithNewMeta(String::toLong, NumberParam)
fun Path.double() = Path.mapWithNewMeta(String::toDouble, NumberParam)
fun Path.float() = Path.mapWithNewMeta(String::toFloat, NumberParam)
fun Path.boolean() = Path.mapWithNewMeta(::safeBooleanFrom, BooleanParam)
fun Path.localDate() = Path.map(java.time.LocalDate::parse)
fun Path.dateTime() = Path.map(java.time.LocalDateTime::parse)
fun Path.zonedDateTime() = Path.map(java.time.ZonedDateTime::parse)
fun Path.uuid() = Path.map(java.util.UUID::fromString)
fun Path.regex(pattern: String, group: Int = 1): PathLensSpec<String> {
    val regex = pattern.toRegex()
    return this.map { regex.matchEntire(it)?.groupValues?.get(group)!! }
}
