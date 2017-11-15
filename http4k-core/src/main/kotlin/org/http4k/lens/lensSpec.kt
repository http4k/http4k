package org.http4k.lens

import org.http4k.lens.ParamMeta.BooleanParam
import org.http4k.lens.ParamMeta.IntegerParam
import org.http4k.lens.ParamMeta.NumberParam
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.UUID


class LensGet<in IN, out OUT> private constructor(private val getFn: (String, IN) -> List<OUT>) {
    operator fun invoke(name: String) = { target: IN -> getFn(name, target) }

    fun <NEXT> map(nextFn: (OUT) -> NEXT) = LensGet({ x, y: IN -> getFn(x, y).map(nextFn) })

    companion object {
        operator fun <IN, OUT> invoke(getFn: (String, IN) -> List<OUT>): LensGet<IN, OUT> = LensGet(getFn)
    }
}

class LensSet<IN, in OUT> private constructor(private val setFn: (String, List<OUT>, IN) -> IN) {
    operator fun invoke(name: String) = { values: List<OUT>, target: IN -> setFn(name, values, target) }

    fun <NEXT> map(nextFn: (NEXT) -> OUT) = LensSet({ a, b: List<NEXT>, c: IN -> setFn(a, b.map(nextFn), c) })

    companion object {
        operator fun <IN, OUT> invoke(setFn: (String, List<OUT>, IN) -> IN): LensSet<IN, OUT> = LensSet(setFn)
    }
}

/**
 * Represents a uni-directional extraction of a list of entities from a target.
 */
interface MultiLensSpec<in IN, OUT> {
    fun defaulted(name: String, default: List<OUT>, description: String? = null): Lens<IN, List<OUT>>
    fun optional(name: String, description: String? = null): Lens<IN, List<OUT>?>
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
    open fun defaulted(name: String, default: OUT, description: String? = null): Lens<IN, OUT> {
        val meta = Meta(false, location, paramMeta, name, description)
        val getLens = get(name)
        return Lens(meta, { getLens(it).let { if (it.isEmpty()) default else it.first() } })
    }

    /**
     * Make a concrete Lens for this spec that looks for an optional value in the target.
     */
    open fun optional(name: String, description: String? = null): Lens<IN, OUT?> {
        val meta = Meta(false, location, paramMeta, name, description)
        val getLens = get(name)
        return Lens(meta, { getLens(it).let { if (it.isEmpty()) null else it.first() } })
    }

    /**
     * Make a concrete Lens for this spec that looks for a required value in the target.
     */
    open fun required(name: String, description: String? = null): Lens<IN, OUT> {
        val meta = Meta(true, location, paramMeta, name, description)
        val getLens = get(name)
        return Lens(meta, { getLens(it).firstOrNull() ?: throw LensFailure(Missing(meta)) })
    }

    open val multi = object : MultiLensSpec<IN, OUT> {
        /**
         * Make a concrete Lens for this spec that falls back to the default list of values if no values are found in the target.
         */
        override fun defaulted(name: String, default: List<OUT>, description: String?): Lens<IN, List<OUT>> {
            val meta = Meta(false, location, paramMeta, name, description)
            val getLens = get(name)
            return Lens(meta, { getLens(it).let { if (it.isEmpty()) default else it } })
        }

        /**
         * Make a concrete Lens for this spec that looks for an optional list of values in the target.
         */
        override fun optional(name: String, description: String?): Lens<IN, List<OUT>?> {
            val meta = Meta(false, location, paramMeta, name, description)
            val getLens = get(name)
            return Lens(meta, { getLens(it).let { if (it.isEmpty()) null else it } })
        }

        /**
         * Make a concrete Lens for this spec that looks for a required list of values in the target.
         */
        override fun required(name: String, description: String?): Lens<IN, List<OUT>> {
            val meta = Meta(true, location, paramMeta, name, description)
            val getLens = get(name)
            return Lens(meta, { getLens(it).let { if (it.isEmpty()) throw LensFailure(Missing(meta)) else it } })
        }
    }
}

/**
 * Represents a bi-directional extraction of a list of entities from a target, or an insertion into a target.
 */
interface BiDiMultiLensSpec<in IN, OUT> : MultiLensSpec<IN, OUT> {
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

    override fun defaulted(name: String, default: OUT, description: String?): BiDiLens<IN, OUT> {
        val meta = Meta(false, location, paramMeta, name, description)
        val getLens = get(name)
        val setLens = set(name)
        return BiDiLens(meta,
            { getLens(it).let { if (it.isEmpty()) default else it.first() } },
            { out: OUT, target: IN -> setLens(out?.let { listOf(it) } ?: emptyList(), target) }
        )
    }

    override fun optional(name: String, description: String?): BiDiLens<IN, OUT?> {
        val meta = Meta(false, location, paramMeta, name, description)
        val getLens = get(name)
        val setLens = set(name)
        return BiDiLens(meta,
            { getLens(it).let { if (it.isEmpty()) null else it.first() } },
            { out: OUT?, target: IN -> setLens(out?.let { listOf(it) } ?: emptyList(), target) }
        )
    }

    override fun required(name: String, description: String?): BiDiLens<IN, OUT> {
        val meta = Meta(true, location, paramMeta, name, description)
        val getLens = get(name)
        val setLens = set(name)
        return BiDiLens(meta,
            { getLens(it).firstOrNull() ?: throw LensFailure(Missing(meta)) },
            { out: OUT, target: IN -> setLens(listOf(out), target) })
    }

    override val multi = object : BiDiMultiLensSpec<IN, OUT> {
        override fun defaulted(name: String, default: List<OUT>, description: String?): BiDiLens<IN, List<OUT>> {
            val meta = Meta(false, location, paramMeta, name, description)
            val getLens = get(name)
            val setLens = set(name)
            return BiDiLens(meta,
                { getLens(it).let { if (it.isEmpty()) default else it } },
                { out: List<OUT>, target: IN -> setLens(out, target) }
            )
        }

        override fun optional(name: String, description: String?): BiDiLens<IN, List<OUT>?> {
            val meta = Meta(false, location, paramMeta, name, description)
            val getLens = get(name)
            val setLens = set(name)
            return BiDiLens(meta,
                { getLens(it).let { if (it.isEmpty()) null else it } },
                { out: List<OUT>?, target: IN -> setLens(out ?: emptyList(), target) }
            )
        }

        override fun required(name: String, description: String?): BiDiLens<IN, List<OUT>> {
            val meta = Meta(true, location, paramMeta, name, description)
            val getLens = get(name)
            val setLens = set(name)
            return BiDiLens(meta,
                { getLens(it).let { if (it.isEmpty()) throw LensFailure(Missing(meta)) else it } },
                { out: List<OUT>, target: IN -> setLens(out, target) })
        }
    }
}

fun <IN> BiDiLensSpec<IN, String>.string() = this
fun <IN> BiDiLensSpec<IN, String>.nonEmptyString() = this.map(::nonEmpty, { it })
fun <IN> BiDiLensSpec<IN, String>.int() = this.mapWithNewMeta(String::toInt, Int::toString, IntegerParam)
fun <IN> BiDiLensSpec<IN, String>.long() = this.mapWithNewMeta(String::toLong, Long::toString, IntegerParam)
fun <IN> BiDiLensSpec<IN, String>.double() = this.mapWithNewMeta(String::toDouble, Double::toString, NumberParam)
fun <IN> BiDiLensSpec<IN, String>.float() = this.mapWithNewMeta(String::toFloat, Float::toString, NumberParam)
fun <IN> BiDiLensSpec<IN, String>.boolean() = this.mapWithNewMeta(::safeBooleanFrom, Boolean::toString, BooleanParam)
fun <IN> BiDiLensSpec<IN, String>.localDate() = this.map(LocalDate::parse, DateTimeFormatter.ISO_LOCAL_DATE::format)
fun <IN> BiDiLensSpec<IN, String>.dateTime() = this.map(LocalDateTime::parse, DateTimeFormatter.ISO_LOCAL_DATE_TIME::format)
fun <IN> BiDiLensSpec<IN, String>.zonedDateTime() = this.map(ZonedDateTime::parse, DateTimeFormatter.ISO_ZONED_DATE_TIME::format)
fun <IN> BiDiLensSpec<IN, String>.uuid() = this.map(UUID::fromString, java.util.UUID::toString)
fun <IN> BiDiLensSpec<IN, String>.regex(pattern: String, group: Int = 1): LensSpec<IN, String> {
    val toRegex = pattern.toRegex()
    return this.map { toRegex.matchEntire(it)?.groupValues?.get(group)!! }
}

internal fun nonEmpty(value: String): String = if (value.isEmpty()) throw IllegalArgumentException() else value

internal fun safeBooleanFrom(value: String): Boolean =
    when {
        value.toUpperCase() == "TRUE" -> true
        value.toUpperCase() == "FALSE" -> false
        else -> throw kotlin.IllegalArgumentException("illegal boolean")
    }