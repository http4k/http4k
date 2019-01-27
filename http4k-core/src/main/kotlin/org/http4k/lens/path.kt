package org.http4k.lens

import org.http4k.core.Request
import org.http4k.core.fromFormEncoded
import org.http4k.core.toPathEncoded
import org.http4k.lens.ParamMeta.BooleanParam
import org.http4k.lens.ParamMeta.NumberParam
import org.http4k.lens.ParamMeta.StringParam
import org.http4k.routing.path
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeFormatter.ISO_LOCAL_DATE
import java.time.format.DateTimeFormatter.ISO_LOCAL_DATE_TIME
import java.time.format.DateTimeFormatter.ISO_LOCAL_TIME
import java.time.format.DateTimeFormatter.ISO_OFFSET_DATE_TIME
import java.time.format.DateTimeFormatter.ISO_OFFSET_TIME
import java.time.format.DateTimeFormatter.ISO_ZONED_DATE_TIME


open class PathLens<out FINAL>(meta: Meta, private val get: (String) -> FINAL) : Lens<Request, FINAL>(meta, {
    it.path(meta.name)?.let(get) ?: throw LensFailure(Missing(meta))
}) {

    operator fun invoke(target: String) = try {
        get(target)
    } catch (e: Exception) {
        throw LensFailure(map { Invalid(it) }, cause = e, target = target)
    }

    override fun toString(): String = "{${meta.name}}"
}

class BiDiPathLens<FINAL>(meta: Meta, get: (String) -> FINAL, private val set: (FINAL, Request) -> Request)
    : LensInjector<FINAL, Request>, PathLens<FINAL>(meta, get) {
    @Suppress("UNCHECKED_CAST")
    override operator fun <R : Request> invoke(value: FINAL, target: R): R = set(value, target) as R
}

/**
 * Represents a uni-directional extraction of an entity from a target path segment.
 */
open class PathLensSpec<out OUT>(protected val paramMeta: ParamMeta, internal val get: LensGet<String, OUT>) {
    open fun of(name: String, description: String? = null): PathLens<OUT> {
        val getLens = get(name)
        val meta = Meta(true, "path", paramMeta, name, description)
        return PathLens(meta) { getLens(it).firstOrNull() ?: throw LensFailure(Missing(meta)) }
    }

    /**
     * Create another PathLensSpec which applies the uni-directional transformation to the result. Any resultant Lens can only be
     * used to extract the final type from a target path segment.
     */
    fun <NEXT> map(nextIn: (OUT) -> NEXT): PathLensSpec<NEXT> = PathLensSpec(paramMeta, get.map(nextIn))
}

open class BiDiPathLensSpec<OUT>(paramMeta: ParamMeta,
                                 get: LensGet<String, OUT>,
                                 private val set: LensSet<Request, OUT>) : PathLensSpec<OUT>(paramMeta, get) {

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

        val meta = Meta(true, "path", paramMeta, name, description)
        return BiDiPathLens(meta,
            { getLens(it).firstOrNull() ?: throw LensFailure(Missing(meta)) },
            { it: OUT, target: Request -> setLens(listOf(it), target) })
    }
}

object Path : BiDiPathLensSpec<String>(StringParam,
    LensGet { _, target -> listOf(target.fromFormEncoded()) },
    LensSet { name, values, target -> target.uri(target.uri.path(target.uri.path.replaceFirst("{$name}", values.first().toPathEncoded()))) }) {

    fun fixed(name: String): PathLens<String> {
        val getLens = get(name)
        val meta = Meta(true, "path", StringParam, name)
        return object : PathLens<String>(meta,
            { getLens(it).find { it == name } ?: throw LensFailure(Missing(meta)) }) {
            override fun toString(): String = name

            override fun iterator(): Iterator<Meta> = emptyList<Meta>().iterator()
        }
    }
}

fun Path.string() = this
fun Path.int() = mapWithNewMeta(BiDiMapping.int(), ParamMeta.IntegerParam)
fun Path.long() = mapWithNewMeta(BiDiMapping.long(), ParamMeta.IntegerParam)
fun Path.double() = mapWithNewMeta(BiDiMapping.double(), NumberParam)
fun Path.float() = mapWithNewMeta(BiDiMapping.float(), NumberParam)
fun Path.boolean() = mapWithNewMeta(BiDiMapping.boolean(), BooleanParam)
fun Path.uuid() = map(BiDiMapping.uuid())
fun Path.uri() = map(BiDiMapping.uri())
fun Path.regex(pattern: String, group: Int = 1) = map(BiDiMapping.regex(pattern, group))
fun Path.regexObject() = map(BiDiMapping.regexObject())
fun Path.duration() = map(BiDiMapping.duration())
fun Path.instant() = map(BiDiMapping.instant())
fun Path.dateTime(formatter: DateTimeFormatter = ISO_LOCAL_DATE_TIME) = map(BiDiMapping.localDateTime(formatter))
fun Path.zonedDateTime(formatter: DateTimeFormatter = ISO_ZONED_DATE_TIME) = map(BiDiMapping.zonedDateTime(formatter))
fun Path.localDate(formatter: DateTimeFormatter = ISO_LOCAL_DATE) = map(BiDiMapping.localDate(formatter))
fun Path.localTime(formatter: DateTimeFormatter = ISO_LOCAL_TIME) = map(BiDiMapping.localTime(formatter))
fun Path.offsetTime(formatter: DateTimeFormatter = ISO_OFFSET_TIME) = map(BiDiMapping.offsetTime(formatter))
fun Path.offsetDateTime(formatter: DateTimeFormatter = ISO_OFFSET_DATE_TIME) = map(BiDiMapping.offsetDateTime(formatter))

internal fun <IN, NEXT> BiDiPathLensSpec<IN>.map(mapping: BiDiMapping<IN, NEXT>) = map(mapping::map, mapping::map)

internal fun <IN, NEXT> BiDiPathLensSpec<IN>.mapWithNewMeta(mapping: BiDiMapping<IN, NEXT>, paramMeta: ParamMeta) = mapWithNewMeta(
    mapping::map, mapping::map, paramMeta)
