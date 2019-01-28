package org.http4k.lens

import org.http4k.lens.ParamMeta.BooleanParam
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
 * Represents a uni-directional extraction of a list of entities from a target.
 */
interface MultiLensSpec<IN, OUT> {
    /**
     * Make a concrete Lens for this spec that fall back to the default list of values if no values are found in the target.
     */
    fun defaulted(name: String, default: List<OUT>, description: String? = null): Lens<IN, List<OUT>>

    /**
     * Make a concrete Lens for this spec that falls back to another lens if no values are found in the target.
     */
    fun defaulted(name: String, default: Lens<IN, List<OUT>>, description: String? = null): Lens<IN, List<OUT>>

    /**
     * Make a concrete Lens for this spec that looks for an optional list of values in the target.
     */
    fun optional(name: String, description: String? = null): Lens<IN, List<OUT>?>

    /**
     * Make a concrete Lens for this spec that looks for a required list of values in the target.
     */
    fun required(name: String, description: String? = null): Lens<IN, List<OUT>>
}

/**
 * Represents a uni-directional extraction of an entity from a target.
 */
open class LensSpec<IN, OUT>(protected val location: String,
                             protected val paramMeta: ParamMeta,
                             internal val get: LensGet<IN, OUT>) {
    /**
     * Create another LensSpec which applies the uni-directional transformation to the result. Any resultant Lens can only be
     * used to extract the final type from a target.
     */
    fun <NEXT> map(nextIn: (OUT) -> NEXT) = LensSpec(location, paramMeta, get.map(nextIn))

    /**
     * Make a concrete Lens for this spec that falls back to the default value if no value is found in the target.
     */
    open fun defaulted(name: String, default: OUT, description: String? = null): Lens<IN, OUT> =
        defaulted(name, Lens(Meta(false, location, paramMeta, name, description)) { default }, description)

    /**
     * Make a concrete Lens for this spec that falls back to another lens if no value is found in the target.
     */
    open fun defaulted(name: String, default: Lens<IN, OUT>, description: String? = null): Lens<IN, OUT> {
        val getLens = get(name)
        return Lens(Meta(false, location, paramMeta, name, description)) { getLens(it).run { if (isEmpty()) default(it) else first() } }
    }

    /**
     * Make a concrete Lens for this spec that looks for an optional value in the target.
     */
    open fun optional(name: String, description: String? = null): Lens<IN, OUT?> {
        val getLens = get(name)
        return Lens(Meta(false, location, paramMeta, name, description)) { getLens(it).run { if (isEmpty()) null else first() } }
    }

    /**
     * Make a concrete Lens for this spec that looks for a required value in the target.
     */
    open fun required(name: String, description: String? = null): Lens<IN, OUT> {
        val meta = Meta(true, location, paramMeta, name, description)
        val getLens = get(name)
        return Lens(meta) { getLens(it).firstOrNull() ?: throw LensFailure(Missing(meta)) }
    }

    open val multi = object : MultiLensSpec<IN, OUT> {
        override fun defaulted(name: String, default: List<OUT>, description: String?): Lens<IN, List<OUT>> =
            defaulted(name, Lens(Meta(false, location, paramMeta, name, description)) { default }, description)

        override fun defaulted(name: String, default: Lens<IN, List<OUT>>, description: String?): Lens<IN, List<OUT>> {
            val getLens = get(name)
            return Lens(Meta(false, location, paramMeta, name, description)) { getLens(it).run { if (isEmpty()) default(it) else this } }
        }

        override fun optional(name: String, description: String?): Lens<IN, List<OUT>?> {
            val getLens = get(name)
            return Lens(Meta(false, location, paramMeta, name, description)) { getLens(it).run { if (isEmpty()) null else this } }
        }

        override fun required(name: String, description: String?): Lens<IN, List<OUT>> {
            val getLens = get(name)
            return Lens(Meta(true, location, paramMeta, name, description)) {
                getLens(it).run {
                    if (isEmpty()) throw LensFailure(Missing(Meta(true, location, paramMeta, name, description))) else this
                }
            }
        }
    }
}

/**
 * Represents a bi-directional extraction of a list of entities from a target, or an insertion into a target.
 */
interface BiDiMultiLensSpec<IN, OUT> : MultiLensSpec<IN, OUT> {
    override fun defaulted(name: String, default: List<OUT>, description: String?): BiDiLens<IN, List<OUT>>
    override fun optional(name: String, description: String?): BiDiLens<IN, List<OUT>?>
    override fun required(name: String, description: String?): BiDiLens<IN, List<OUT>>
}

/**
 * Represents a bi-directional extraction of an entity from a target, or an insertion into a target.
 */
open class BiDiLensSpec<IN, OUT>(location: String,
                                 paramMeta: ParamMeta,
                                 get: LensGet<IN, OUT>,
                                 private val set: LensSet<IN, OUT>) : LensSpec<IN, OUT>(location, paramMeta, get) {

    /**
     * Create another BiDiLensSpec which applies the bi-directional transformations to the result. Any resultant Lens can be
     * used to extract or insert the final type from/into a target.
     */
    fun <NEXT> map(nextIn: (OUT) -> NEXT, nextOut: (NEXT) -> OUT) = mapWithNewMeta(nextIn, nextOut, paramMeta)

    internal fun <NEXT> mapWithNewMeta(nextIn: (OUT) -> NEXT, nextOut: (NEXT) -> OUT, paramMeta: ParamMeta) = BiDiLensSpec(location, paramMeta, get.map(nextIn), set.map(nextOut))

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
                    ?: throw LensFailure(Missing(Meta(true, location, paramMeta, name, description)))
            },
            { out: OUT, target: IN -> setLens(listOf(out), target) })
    }

    override val multi = object : BiDiMultiLensSpec<IN, OUT> {
        override fun defaulted(name: String, default: List<OUT>, description: String?): BiDiLens<IN, List<OUT>> =
            defaulted(name, Lens(Meta(false, location, paramMeta, name, description)) { default })

        override fun defaulted(name: String, default: Lens<IN, List<OUT>>, description: String?): BiDiLens<IN, List<OUT>> {
            val getLens = get(name)
            val setLens = set(name)
            return BiDiLens(Meta(false, location, paramMeta, name, description),
                { getLens(it).run { if (isEmpty()) default(it) else this } },
                { out: List<OUT>, target: IN -> setLens(out, target) }
            )
        }

        override fun optional(name: String, description: String?): BiDiLens<IN, List<OUT>?> {
            val getLens = get(name)
            val setLens = set(name)
            return BiDiLens(Meta(false, location, paramMeta, name, description),
                { getLens(it).run { if (isEmpty()) null else this } },
                { out: List<OUT>?, target: IN -> setLens(out ?: emptyList(), target) }
            )
        }

        override fun required(name: String, description: String?): BiDiLens<IN, List<OUT>> {
            val getLens = get(name)
            val setLens = set(name)
            return BiDiLens(Meta(true, location, paramMeta, name, description),
                { getLens(it).run { if (isEmpty()) throw LensFailure(Missing(Meta(true, location, paramMeta, name, description))) else this } },
                { out: List<OUT>, target: IN -> setLens(out, target) })
        }
    }
}

fun <IN> BiDiLensSpec<IN, String>.string() = this
fun <IN> BiDiLensSpec<IN, String>.nonEmptyString() = map(BiDiMapping.nonEmptyString())
fun <IN> BiDiLensSpec<IN, String>.int() = mapWithNewMeta(BiDiMapping.int(), IntegerParam)
fun <IN> BiDiLensSpec<IN, String>.long() = mapWithNewMeta(BiDiMapping.long(), IntegerParam)
fun <IN> BiDiLensSpec<IN, String>.double() = mapWithNewMeta(BiDiMapping.double(), NumberParam)
fun <IN> BiDiLensSpec<IN, String>.float() = mapWithNewMeta(BiDiMapping.float(), NumberParam)
fun <IN> BiDiLensSpec<IN, String>.boolean() = mapWithNewMeta(BiDiMapping.boolean(), BooleanParam)
fun <IN> BiDiLensSpec<IN, String>.bigInteger() = mapWithNewMeta(BiDiMapping.bigInteger(), IntegerParam)
fun <IN> BiDiLensSpec<IN, String>.bigDecimal() = mapWithNewMeta(BiDiMapping.bigDecimal(), NumberParam)
fun <IN> BiDiLensSpec<IN, String>.uuid() = map(BiDiMapping.uuid())
fun <IN> BiDiLensSpec<IN, String>.uri() = map(BiDiMapping.uri())
fun <IN> BiDiLensSpec<IN, String>.regex(pattern: String, group: Int = 1) = map(BiDiMapping.regex(pattern, group))
fun <IN> BiDiLensSpec<IN, String>.regexObject() = map(BiDiMapping.regexObject())
fun <IN> BiDiLensSpec<IN, String>.duration() = map(BiDiMapping.duration())
fun <IN> BiDiLensSpec<IN, String>.instant() = map(BiDiMapping.instant())
fun <IN> BiDiLensSpec<IN, String>.dateTime(formatter: DateTimeFormatter = ISO_LOCAL_DATE_TIME) = map(BiDiMapping.localDateTime(formatter))
fun <IN> BiDiLensSpec<IN, String>.zonedDateTime(formatter: DateTimeFormatter = ISO_ZONED_DATE_TIME) = map(BiDiMapping.zonedDateTime(formatter))
fun <IN> BiDiLensSpec<IN, String>.localDate(formatter: DateTimeFormatter = ISO_LOCAL_DATE) = map(BiDiMapping.localDate(formatter))
fun <IN> BiDiLensSpec<IN, String>.localTime(formatter: DateTimeFormatter = ISO_LOCAL_TIME) = map(BiDiMapping.localTime(formatter))
fun <IN> BiDiLensSpec<IN, String>.offsetTime(formatter: DateTimeFormatter = ISO_OFFSET_TIME) = map(BiDiMapping.offsetTime(formatter))
fun <IN> BiDiLensSpec<IN, String>.offsetDateTime(formatter: DateTimeFormatter = ISO_OFFSET_DATE_TIME) = map(BiDiMapping.offsetDateTime(formatter))

internal fun <NEXT, IN, OUT> BiDiLensSpec<IN, OUT>.mapWithNewMeta(mapping: BiDiMapping<OUT, NEXT>, paramMeta: ParamMeta) = mapWithNewMeta(
    mapping::invoke, mapping::invoke, paramMeta)

internal fun <NEXT, IN, OUT> BiDiLensSpec<IN, OUT>.map(mapping: BiDiMapping<OUT, NEXT>) = map(mapping::invoke, mapping::invoke)
