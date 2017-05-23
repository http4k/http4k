package org.http4k.lens

import org.http4k.core.Request
import org.http4k.core.decode
import org.http4k.core.encode
import org.http4k.lens.ParamMeta.BooleanParam
import org.http4k.lens.ParamMeta.NumberParam
import org.http4k.lens.ParamMeta.StringParam
import org.http4k.routing.path
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.*

open class PathLens<out FINAL>(meta: Meta, private val get: (String) -> FINAL) : Lens<Request, FINAL>(meta, {
    it.path(meta.name)?.let(get) ?: throw LensFailure(meta.missing())
}) {

    operator fun invoke(target: String) = try {
        get(target)
    } catch (e: Exception) {
        throw LensFailure(map { it.invalid() }, cause = e)
    }

    override fun toString(): String = "{${meta.name}}"
}

class BiDiPathLens<FINAL>(meta: Meta, get: (String) -> FINAL, private val set: (FINAL, Request) -> Request)
    : LensInjector<Request, FINAL>, PathLens<FINAL>(meta, get) {
    @Suppress("UNCHECKED_CAST")
    override operator fun <R : Request> invoke(value: FINAL, target: R): R = set(value, target) as R
}

/**
 * Represents a uni-directional extraction of an entity from a target path segment.
 */
open class PathLensSpec<out OUT>(protected val paramMeta: ParamMeta, internal val get: LensGet<String, String, OUT>) {
    open fun of(name: String, description: String? = null): PathLens<OUT> {
        val getLens = get(name)
        return PathLens(Meta(true, "path", paramMeta, name, description),
            { getLens(it).firstOrNull() ?: throw LensFailure() })
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
        val getLens = get(name)
        val setLens = set(name)

        return BiDiPathLens(Meta(true, "path", paramMeta, name, description),
            { getLens(it).firstOrNull() ?: throw LensFailure() },
            { it: OUT, target: Request -> setLens(listOf(it), target) })
    }
}

object Path : BiDiPathLensSpec<String>(StringParam,
    LensGet { _, target -> listOf(target.decode()) },
    LensSet { name, values, target -> target.uri(target.uri.path(target.uri.path.replaceFirst("{$name}", values.first().encode()))) }) {

    fun fixed(name: String): PathLens<String> {
        val getLens = get(name)
        return object : PathLens<String>(Meta(true, "path", StringParam, name),
            { getLens(it).find { it == name } ?: throw LensFailure() }) {
            override fun toString(): String = name

            override fun iterator(): Iterator<Meta> = emptyList<Meta>().iterator()
        }
    }
}

fun Path.string() = this
fun Path.nonEmptyString() = this.map(::nonEmpty, { it })
fun Path.int() = this.mapWithNewMeta(String::toInt, Int::toString, NumberParam)
fun Path.long() = this.mapWithNewMeta(String::toLong, Long::toString, NumberParam)
fun Path.double() = this.mapWithNewMeta(String::toDouble, Double::toString, NumberParam)
fun Path.float() = this.mapWithNewMeta(String::toFloat, Float::toString, NumberParam)
fun Path.boolean() = this.mapWithNewMeta(::safeBooleanFrom, Boolean::toString, BooleanParam)
fun Path.localDate() = this.map(LocalDate::parse, DateTimeFormatter.ISO_LOCAL_DATE::format)
fun Path.dateTime() = this.map(LocalDateTime::parse, DateTimeFormatter.ISO_LOCAL_DATE_TIME::format)
fun Path.zonedDateTime() = this.map(ZonedDateTime::parse, DateTimeFormatter.ISO_ZONED_DATE_TIME::format)
fun Path.uuid() = this.map(UUID::fromString, java.util.UUID::toString)
fun Path.regex(pattern: String, group: Int = 1): PathLensSpec<String> {
    val toRegex = pattern.toRegex()
    return this.map { toRegex.matchEntire(it)?.groupValues?.get(group)!! }
}
