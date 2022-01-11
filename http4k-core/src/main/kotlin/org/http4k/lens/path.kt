package org.http4k.lens

import org.http4k.core.Request
import org.http4k.core.toPathEncoded
import org.http4k.lens.ParamMeta.BooleanParam
import org.http4k.lens.ParamMeta.EnumParam
import org.http4k.lens.ParamMeta.IntegerParam
import org.http4k.lens.ParamMeta.NumberParam
import org.http4k.lens.ParamMeta.StringParam
import org.http4k.routing.path
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeFormatter.ISO_LOCAL_DATE
import java.time.format.DateTimeFormatter.ISO_LOCAL_DATE_TIME
import java.time.format.DateTimeFormatter.ISO_LOCAL_TIME
import java.time.format.DateTimeFormatter.ISO_ZONED_DATE_TIME

open class PathLens<out FINAL>(meta: Meta, private val get: (String) -> FINAL) : Lens<Request, FINAL>(meta, {
    it.path(meta.name)?.let(get) ?: throw LensFailure(Missing(meta), target = it)
}) {

    operator fun invoke(target: String) = try {
        get(target)
    } catch (e: Exception) {
        throw LensFailure(map { Invalid(it) }, cause = e, target = target)
    }

    override fun toString(): String = "{${meta.name}}"
}

class BiDiPathLens<FINAL>(
    meta: Meta,
    get: (String) -> FINAL,
    private val set: (FINAL, Request) -> Request
) : LensInjector<FINAL, Request>, PathLens<FINAL>(meta, get) {
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
        return PathLens(meta) { getLens(it).firstOrNull() ?: throw LensFailure(Missing(meta), target = it) }
    }

    /**
     * Create another PathLensSpec which applies the uni-directional transformation to the result. Any resultant Lens can only be
     * used to extract the final type from a target path segment.
     */
    fun <NEXT> map(nextIn: (OUT) -> NEXT): PathLensSpec<NEXT> = PathLensSpec(paramMeta, get.map(nextIn))
}

open class BiDiPathLensSpec<OUT>(
    paramMeta: ParamMeta,
    get: LensGet<String, OUT>,
    private val set: LensSet<Request, OUT>
) : PathLensSpec<OUT>(paramMeta, get) {

    /**
     * Create another BiDiPathLensSpec which applies the bi-directional transformations to the result. Any resultant Lens can be
     * used to extract or insert the final type from/into a path segment.
     */
    fun <NEXT> map(nextIn: (OUT) -> NEXT, nextOut: (NEXT) -> OUT) =
        BiDiPathLensSpec(paramMeta, get.map(nextIn), set.map(nextOut))

    internal fun <NEXT> mapWithNewMeta(
        nextIn: (OUT) -> NEXT,
        nextOut: (NEXT) -> OUT,
        newMeta: ParamMeta
    ): BiDiPathLensSpec<NEXT> = BiDiPathLensSpec(newMeta, get.map(nextIn), set.map(nextOut))

    /**
     * Create a lens for this Spec
     */
    override fun of(name: String, description: String?): BiDiPathLens<OUT> {
        val getLens = get(name)
        val setLens = set(name)

        val meta = Meta(true, "path", paramMeta, name, description)
        return BiDiPathLens(meta,
            { getLens(it).firstOrNull() ?: throw LensFailure(Missing(meta), target = it) },
            { it: OUT, target: Request -> setLens(listOf(it), target) })
    }
}

object Path : BiDiPathLensSpec<String>(StringParam,
    LensGet { _, target -> listOf(target) },
    LensSet { name, values, target ->
        target.uri(
            target.uri.path(
                target.uri.path.replaceFirst(
                    "{$name}",
                    values.first().toPathEncoded()
                )
            )
        )
    }) {

    fun fixed(name: String): PathLens<String> {
        if (name.contains('/')) throw IllegalArgumentException("""Fixed path segments cannot contain /. Use the "a / b" form.""")
        val getLens = get(name)
        val meta = Meta(true, "path", StringParam, name)
        return object : PathLens<String>(meta,
            { getLens(it).find { it == name } ?: throw LensFailure(Missing(meta), target = it) }) {
            override fun toString(): String = name

            override fun iterator(): Iterator<Meta> = emptyList<Meta>().iterator()
        }
    }
}

fun Path.string() = this
fun Path.nonEmptyString() = map(StringBiDiMappings.nonEmpty())
fun Path.int() = mapWithNewMeta(StringBiDiMappings.int(), IntegerParam)
fun Path.long() = mapWithNewMeta(StringBiDiMappings.long(), IntegerParam)
fun Path.double() = mapWithNewMeta(StringBiDiMappings.double(), NumberParam)
fun Path.float() = mapWithNewMeta(StringBiDiMappings.float(), NumberParam)
fun Path.bigInteger() = mapWithNewMeta(StringBiDiMappings.bigInteger(), IntegerParam)
fun Path.bigDecimal() = mapWithNewMeta(StringBiDiMappings.bigDecimal(), NumberParam)
fun Path.boolean() = mapWithNewMeta(StringBiDiMappings.boolean(), BooleanParam)
fun Path.base64() = map(StringBiDiMappings.base64())
fun Path.uuid() = map(StringBiDiMappings.uuid())
fun Path.uri() = map(StringBiDiMappings.uri())
fun Path.regex(pattern: String, group: Int = 1) = map(StringBiDiMappings.regex(pattern, group))
fun Path.regexObject() = map(StringBiDiMappings.regexObject())
fun Path.duration() = map(StringBiDiMappings.duration())
fun Path.yearMonth() = map(StringBiDiMappings.yearMonth())
fun Path.instant() = map(StringBiDiMappings.instant())
fun Path.dateTime(formatter: DateTimeFormatter = ISO_LOCAL_DATE_TIME) = map(StringBiDiMappings.localDateTime(formatter))
fun Path.zonedDateTime(formatter: DateTimeFormatter = ISO_ZONED_DATE_TIME) =
    map(StringBiDiMappings.zonedDateTime(formatter))

fun Path.localDate(formatter: DateTimeFormatter = ISO_LOCAL_DATE) = map(StringBiDiMappings.localDate(formatter))
fun Path.localTime(formatter: DateTimeFormatter = ISO_LOCAL_TIME) = map(StringBiDiMappings.localTime(formatter))

inline fun <reified T : Enum<T>> Path.enum() = mapWithNewMeta(StringBiDiMappings.enum<T>(), EnumParam(T::class))
inline fun <reified T : Enum<T>> Path.enum(noinline nextOut: (String) -> T, noinline nextIn: (T) -> String) = mapWithNewMeta(BiDiMapping(nextOut, nextIn), EnumParam(T::class))

@PublishedApi
internal fun <IN, NEXT> BiDiPathLensSpec<IN>.map(mapping: BiDiMapping<IN, NEXT>) = map(mapping::invoke, mapping::invoke)

fun <IN, NEXT> BiDiPathLensSpec<IN>.mapWithNewMeta(mapping: BiDiMapping<IN, NEXT>, paramMeta: ParamMeta) =
    mapWithNewMeta(mapping::invoke, mapping::invoke, paramMeta)
