package org.http4k.lens

import org.http4k.lens.ParamMeta.ArrayParam
import org.http4k.lens.ParamMeta.BooleanParam
import org.http4k.lens.ParamMeta.EnumParam
import org.http4k.lens.ParamMeta.IntegerParam
import org.http4k.lens.ParamMeta.NumberParam
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeFormatter.ISO_LOCAL_DATE
import java.time.format.DateTimeFormatter.ISO_LOCAL_DATE_TIME
import java.time.format.DateTimeFormatter.ISO_LOCAL_TIME
import java.time.format.DateTimeFormatter.ISO_OFFSET_DATE_TIME
import java.time.format.DateTimeFormatter.ISO_OFFSET_TIME
import java.time.format.DateTimeFormatter.ISO_ZONED_DATE_TIME

class LensGet<in IN, out OUT> private constructor(private val getFn: (String, IN) -> List<OUT>) {
    operator fun invoke(name: String) = { target: IN -> getFn(name, target) }

    fun <NEXT> map(nextFn: (OUT) -> NEXT) = LensGet { x, y: IN -> getFn(x, y).map(nextFn) }

    companion object {
        operator fun <IN, OUT> invoke(getFn: (String, IN) -> List<OUT>): LensGet<IN, OUT> = LensGet(getFn)
    }
}

class LensSet<IN, in OUT> private constructor(private val setFn: (String, List<OUT>, IN) -> IN) {
    operator fun invoke(name: String) = { values: List<OUT>, target: IN -> setFn(name, values, target) }

    fun <NEXT> map(nextFn: (NEXT) -> OUT) = LensSet { a, b: List<NEXT>, c: IN -> setFn(a, b.map(nextFn), c) }

    companion object {
        operator fun <IN, OUT> invoke(setFn: (String, List<OUT>, IN) -> IN): LensSet<IN, OUT> = LensSet(setFn)
    }
}

/**
 * Common construction patterns for all lens implementations.
 */
interface LensBuilder<IN : Any, OUT, Req : Lens<IN, OUT>, Opt : Lens<IN, OUT?>> {

    /**
     * Make a concrete Lens for this spec that looks for an optional value in the target.
     */
    fun optional(name: String, description: String? = null): Opt

    /**
     * Make a concrete Lens for this spec that looks for a required value in the target.
     */
    fun required(name: String, description: String? = null): Req

    /**
     * Make a concrete Lens for this spec that falls back to the default value if no value is found in the target.
     */
    fun defaulted(name: String, default: OUT, description: String? = null): Req

    /**
     * Make a concrete Lens for this spec that falls back to another lens if no value is found in the target.
     */
    fun defaulted(name: String, default: Lens<IN, OUT>, description: String? = null): Req
}

/**
 * Represents a uni-directional extraction of a list of entities from a target.
 */
typealias MultiLensSpec<IN, OUT> = LensBuilder<IN, List<OUT>, Lens<IN, List<OUT>>, Lens<IN, List<OUT>?>>

/**
 * Represents a uni-directional extraction of an entity from a target.
 */
open class LensSpec<IN : Any, OUT>(
    val location: String,
    protected val paramMeta: ParamMeta,
    internal val get: LensGet<IN, OUT>
) : LensBuilder<IN, OUT, Lens<IN, OUT>, Lens<IN, OUT?>> {
    /**
     * Create another LensSpec which applies the uni-directional transformation to the result. Any resultant Lens can only be
     * used to extract the final type from a target.
     */
    fun <NEXT> map(nextIn: (OUT) -> NEXT) = LensSpec(location, paramMeta, get.map(nextIn))

    override fun defaulted(name: String, default: OUT, description: String?): Lens<IN, OUT> =
        defaulted(name, Lens(Meta(false, location, paramMeta, name, description)) { default }, description)

    override fun defaulted(name: String, default: Lens<IN, OUT>, description: String?): Lens<IN, OUT> {
        val getLens = get(name)
        return Lens(
            Meta(
                false,
                location,
                paramMeta,
                name,
                description
            )
        ) { getLens(it).run { if (isEmpty()) default(it) else first() } }
    }

    override fun optional(name: String, description: String?): Lens<IN, OUT?> {
        val getLens = get(name)
        return Lens(
            Meta(
                false,
                location,
                paramMeta,
                name,
                description
            )
        ) { getLens(it).run { if (isEmpty()) null else first() } }
    }

    override fun required(name: String, description: String?): Lens<IN, OUT> {
        val meta = Meta(true, location, paramMeta, name, description)
        val getLens = get(name)
        return Lens(meta) { getLens(it).firstOrNull() ?: throw LensFailure(listOf(Missing(meta)), target = it) }
    }

    open val multi = object : MultiLensSpec<IN, OUT> {
        override fun defaulted(name: String, default: List<OUT>, description: String?): Lens<IN, List<OUT>> =
            defaulted(
                name,
                Lens(Meta(false, location, ArrayParam(paramMeta), name, description)) { default },
                description
            )

        override fun defaulted(name: String, default: Lens<IN, List<OUT>>, description: String?): Lens<IN, List<OUT>> {
            val getLens = get(name)
            return Lens(
                Meta(
                    false,
                    location,
                    ArrayParam(paramMeta),
                    name,
                    description
                )
            ) { getLens(it).run { ifEmpty { default(it) } } }
        }

        override fun optional(name: String, description: String?): Lens<IN, List<OUT>?> {
            val getLens = get(name)
            return Lens(
                Meta(
                    false,
                    location,
                    ArrayParam(paramMeta),
                    name,
                    description
                )
            ) { getLens(it).run { ifEmpty { null } } }
        }

        override fun required(name: String, description: String?): Lens<IN, List<OUT>> {
            val getLens = get(name)
            return Lens(Meta(true, location, ArrayParam(paramMeta), name, description)) {
                getLens(it).run {
                    ifEmpty {
                        throw LensFailure(
                            Missing(Meta(true, location, paramMeta, name, description)),
                            target = it
                        )
                    }
                }
            }
        }
    }
}

/**
 * Represents a bi-directional extraction of a list of entities from a target, or an insertion into a target.
 */
interface BiDiMultiLensSpec<IN : Any, OUT> : MultiLensSpec<IN, OUT> {
    override fun defaulted(name: String, default: List<OUT>, description: String?): BiDiLens<IN, List<OUT>>
    override fun optional(name: String, description: String?): BiDiLens<IN, List<OUT>?>
    override fun required(name: String, description: String?): BiDiLens<IN, List<OUT>>
}

/**
 * Represents a bi-directional extraction of an entity from a target, or an insertion into a target.
 */
open class BiDiLensSpec<IN : Any, OUT>(
    location: String,
    paramMeta: ParamMeta,
    get: LensGet<IN, OUT>,
    private val set: LensSet<IN, OUT>
) : LensSpec<IN, OUT>(location, paramMeta, get) {

    /**
     * Create another BiDiLensSpec which applies the bi-directional transformations to the result. Any resultant Lens can be
     * used to extract or insert the final type from/into a target.
     */
    fun <NEXT> map(nextIn: (OUT) -> NEXT, nextOut: (NEXT) -> OUT) = mapWithNewMeta(nextIn, nextOut, paramMeta)

    fun <NEXT> mapWithNewMeta(nextIn: (OUT) -> NEXT, nextOut: (NEXT) -> OUT, paramMeta: ParamMeta) =
        BiDiLensSpec(location, paramMeta, get.map(nextIn), set.map(nextOut))

    override fun defaulted(name: String, default: OUT, description: String?) =
        defaulted(name, Lens(Meta(false, location, paramMeta, name, description)) { default }, description)

    override fun defaulted(name: String, default: Lens<IN, OUT>, description: String?): BiDiLens<IN, OUT> {
        val getLens = get(name)
        val setLens = set(name)
        return BiDiLens(Meta(false, location, paramMeta, name, description),
            { getLens(it).run { if (isEmpty()) default(it) else first() } },
            { out: OUT, target: IN -> setLens(out?.let { listOf(it) } ?: emptyList(), target) }
        )
    }

    override fun optional(name: String, description: String?): BiDiLens<IN, OUT?> {
        val getLens = get(name)
        val setLens = set(name)
        return BiDiLens(Meta(false, location, paramMeta, name, description),
            { getLens(it).run { if (isEmpty()) null else first() } },
            { out: OUT?, target: IN -> setLens(out?.let { listOf(it) } ?: emptyList(), target) }
        )
    }

    override fun required(name: String, description: String?): BiDiLens<IN, OUT> {
        val getLens = get(name)
        val setLens = set(name)
        return BiDiLens(Meta(true, location, paramMeta, name, description),
            {
                getLens(it).firstOrNull()
                    ?: throw LensFailure(Missing(Meta(true, location, paramMeta, name, description)), target = it)
            },
            { out: OUT, target: IN -> setLens(listOf(out), target) })
    }

    override val multi = object : BiDiMultiLensSpec<IN, OUT> {
        override fun defaulted(name: String, default: List<OUT>, description: String?): BiDiLens<IN, List<OUT>> =
            defaulted(
                name,
                Lens(Meta(false, location, ArrayParam(paramMeta), name, description)) { default },
                description
            )

        override fun defaulted(
            name: String,
            default: Lens<IN, List<OUT>>,
            description: String?
        ): BiDiLens<IN, List<OUT>> {
            val getLens = get(name)
            val setLens = set(name)
            return BiDiLens(Meta(false, location, ArrayParam(paramMeta), name, description),
                { getLens(it).run { ifEmpty { default(it) } } },
                { out: List<OUT>, target: IN -> setLens(out, target) }
            )
        }

        override fun optional(name: String, description: String?): BiDiLens<IN, List<OUT>?> {
            val getLens = get(name)
            val setLens = set(name)
            return BiDiLens(Meta(false, location, ArrayParam(paramMeta), name, description),
                { getLens(it).run { ifEmpty { null } } },
                { out: List<OUT>?, target: IN -> setLens(out ?: emptyList(), target) }
            )
        }

        override fun required(name: String, description: String?): BiDiLens<IN, List<OUT>> {
            val getLens = get(name)
            val setLens = set(name)
            return BiDiLens(Meta(true, location, ArrayParam(paramMeta), name, description),
                {
                    getLens(it).run {
                        ifEmpty {
                            throw LensFailure(
                                Missing(
                                    Meta(
                                        true,
                                        location,
                                        ArrayParam(paramMeta),
                                        name,
                                        description
                                    )
                                ), target = it
                            )
                        }
                    }
                },
                { out: List<OUT>, target: IN -> setLens(out, target) })
        }
    }
}

fun <IN : Any> BiDiLensSpec<IN, String>.string() = this
fun <IN : Any> BiDiLensSpec<IN, String>.nonEmptyString() = map(StringBiDiMappings.nonEmpty())
fun <IN : Any> BiDiLensSpec<IN, String>.int() = mapWithNewMeta(StringBiDiMappings.int(), IntegerParam)
fun <IN : Any> BiDiLensSpec<IN, String>.long() = mapWithNewMeta(StringBiDiMappings.long(), IntegerParam)
fun <IN : Any> BiDiLensSpec<IN, String>.double() = mapWithNewMeta(StringBiDiMappings.double(), NumberParam)
fun <IN : Any> BiDiLensSpec<IN, String>.float() = mapWithNewMeta(StringBiDiMappings.float(), NumberParam)
fun <IN : Any> BiDiLensSpec<IN, String>.boolean() = mapWithNewMeta(StringBiDiMappings.boolean(), BooleanParam)
fun <IN : Any> BiDiLensSpec<IN, String>.bigInteger() = mapWithNewMeta(StringBiDiMappings.bigInteger(), IntegerParam)
fun <IN : Any> BiDiLensSpec<IN, String>.bigDecimal() = mapWithNewMeta(StringBiDiMappings.bigDecimal(), NumberParam)
fun <IN : Any> BiDiLensSpec<IN, String>.uuid() = map(StringBiDiMappings.uuid())
fun <IN : Any> BiDiLensSpec<IN, String>.uri() = map(StringBiDiMappings.uri())
fun <IN : Any> BiDiLensSpec<IN, String>.bytes() = map { s: String -> s.toByteArray() }
fun <IN : Any> BiDiLensSpec<IN, String>.regex(pattern: String, group: Int = 1) =
    map(StringBiDiMappings.regex(pattern, group))

fun <IN : Any> BiDiLensSpec<IN, String>.regexObject() = map(StringBiDiMappings.regexObject())
fun <IN : Any> BiDiLensSpec<IN, String>.duration() = map(StringBiDiMappings.duration())
fun <IN : Any> BiDiLensSpec<IN, String>.base64() = map(StringBiDiMappings.base64())
fun <IN : Any> BiDiLensSpec<IN, String>.instant() = map(StringBiDiMappings.instant())
fun <IN : Any> BiDiLensSpec<IN, String>.yearMonth() = map(StringBiDiMappings.yearMonth())
fun <IN : Any> BiDiLensSpec<IN, String>.dateTime(formatter: DateTimeFormatter = ISO_LOCAL_DATE_TIME) =
    map(StringBiDiMappings.localDateTime(formatter))

fun <IN : Any> BiDiLensSpec<IN, String>.zonedDateTime(formatter: DateTimeFormatter = ISO_ZONED_DATE_TIME) =
    map(StringBiDiMappings.zonedDateTime(formatter))

fun <IN : Any> BiDiLensSpec<IN, String>.localDate(formatter: DateTimeFormatter = ISO_LOCAL_DATE) =
    map(StringBiDiMappings.localDate(formatter))

fun <IN : Any> BiDiLensSpec<IN, String>.localTime(formatter: DateTimeFormatter = ISO_LOCAL_TIME) =
    map(StringBiDiMappings.localTime(formatter))

fun <IN : Any> BiDiLensSpec<IN, String>.offsetTime(formatter: DateTimeFormatter = ISO_OFFSET_TIME) =
    map(StringBiDiMappings.offsetTime(formatter))

fun <IN : Any> BiDiLensSpec<IN, String>.offsetDateTime(formatter: DateTimeFormatter = ISO_OFFSET_DATE_TIME) =
    map(StringBiDiMappings.offsetDateTime(formatter))

fun <IN : Any> BiDiLensSpec<IN, String>.zoneId() = map(StringBiDiMappings.zoneId())
fun <IN : Any> BiDiLensSpec<IN, String>.zoneOffset() = map(StringBiDiMappings.zoneOffset())
fun <IN : Any> BiDiLensSpec<IN, String>.locale() = map(StringBiDiMappings.locale())
fun <IN : Any> BiDiLensSpec<IN, String>.basicCredentials() = map(StringBiDiMappings.basicCredentials())
fun <IN : Any, T : Any> BiDiLensSpec<IN, String>.csv(delimiter: String = ",", mapElement: BiDiMapping<String, T>) =
    map(StringBiDiMappings.csv(delimiter, mapElement))

fun <IN : Any> BiDiLensSpec<IN, String>.csv(delimiter: String = ",") = csv(delimiter, BiDiMapping({ it }, { it }))

inline fun <IN : Any, reified T : Enum<T>> BiDiLensSpec<IN, String>.enum() =
    mapWithNewMeta(StringBiDiMappings.enum<T>(), EnumParam(T::class))

inline fun <IN : Any, reified T : Enum<T>> BiDiLensSpec<IN, String>.enum(
    noinline nextOut: (String) -> T,
    noinline nextIn: (T) -> String
) = mapWithNewMeta(BiDiMapping(nextOut, nextIn), EnumParam(T::class))

fun <NEXT, IN : Any, OUT> BiDiLensSpec<IN, OUT>.mapWithNewMeta(mapping: BiDiMapping<OUT, NEXT>, paramMeta: ParamMeta) =
    mapWithNewMeta(
        mapping::invoke, mapping::invoke, paramMeta
    )

fun <NEXT, IN : Any, OUT> BiDiLensSpec<IN, OUT>.map(mapping: BiDiMapping<OUT, NEXT>) =
    map(mapping::invoke, mapping::invoke)

/**
 * This allows creation of a composite object from several values from the same source.
 */
inline fun <TARGET : Any, reified T> BiDiLensSpec<TARGET, String>.composite(crossinline fn: BiDiLensSpec<TARGET, String>.(TARGET) -> T) =
    LensSpec<TARGET, T>(
        T::class.java.name,
        ParamMeta.ObjectParam,
        LensGet { _, target -> listOf(fn(target)) }).required(T::class.java.name)

inline fun <TARGET : Any, reified T> BiDiLensSpec<TARGET, String>.composite(
    crossinline getFn: BiDiLensSpec<TARGET, String>.(TARGET) -> T,
    crossinline setFn: T.(TARGET) -> TARGET
) = BiDiLensSpec(
    T::class.java.name, ParamMeta.ObjectParam,
    LensGet { _, target -> listOf(getFn(target)) },
    LensSet<TARGET, T> { _, values, target -> values.fold(target) { msg, next: T -> next.setFn(msg) } })
    .required(T::class.java.name)
