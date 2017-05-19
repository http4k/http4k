package org.http4k.lens

import org.http4k.lens.ParamMeta.BooleanParam
import org.http4k.lens.ParamMeta.NumberParam
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.*

class LensGet<in IN, MID, out OUT> private constructor(private val rootFn: (String, IN) -> List<MID>, private val fn: (MID) -> OUT) {
    operator fun invoke(name: String) = { target: IN -> rootFn(name, target).map(fn) }

    fun <NEXT> map(nextFn: (OUT) -> NEXT) = LensGet(rootFn, { nextFn(fn(it)) })

    companion object {
        operator fun <IN, OUT> invoke(rootFn: (String, IN) -> List<OUT>): LensGet<IN, OUT, OUT> = LensGet(rootFn, { it })
    }
}

class LensSet<IN, MID, in OUT> private constructor(private val rootFn: (String, List<MID>, IN) -> IN, private val fn: (OUT) -> MID) {
    operator fun invoke(name: String) = { values: List<OUT>, target: IN -> rootFn(name, values.map(fn), target) }

    fun <NEXT> map(nextFn: (NEXT) -> OUT) = LensSet(rootFn, { value: NEXT -> fn(nextFn(value)) })

    companion object {
        operator fun <IN, OUT> invoke(rootFn: (String, List<OUT>, IN) -> IN): LensSet<IN, OUT, OUT> = LensSet(rootFn, { it })
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
open class LensSpec<IN, MID, OUT>(protected val location: String,
                                  protected val paramMeta: ParamMeta,
                                  internal val get: LensGet<IN, MID, OUT>) {
    /**
     * Create another LensSpec which applies the uni-directional transformation to the result. Any resultant Lens can only be
     * used to extract the final type from a target.
     */
    fun <NEXT> map(nextIn: (OUT) -> NEXT) = mapWithNewMeta(nextIn, paramMeta)

    internal fun <NEXT> mapWithNewMeta(nextIn: (OUT) -> NEXT, paramMeta: ParamMeta) = LensSpec(location, paramMeta, get.map(nextIn))

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
        return Lens(meta, { getLens(it).firstOrNull() ?: throw LensFailure(meta.missing()) })
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
            return Lens(meta, { getLens(it).let { if (it.isEmpty()) throw LensFailure(meta.missing()) else it } })
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
open class BiDiLensSpec<IN, MID, OUT>(location: String,
                                      paramMeta: ParamMeta,
                                      get: LensGet<IN, MID, OUT>,
                                      private val set: LensSet<IN, MID, OUT>) : LensSpec<IN, MID, OUT>(location, paramMeta, get) {

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
            { getLens(it).firstOrNull() ?: throw LensFailure(meta.missing()) },
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
                { getLens(it).let { if (it.isEmpty()) throw LensFailure(meta.missing()) else it } },
                { out: List<OUT>, target: IN -> setLens(out, target) })
        }
    }
}

fun <IN> BiDiLensSpec<IN, String, String>.string() = this
fun <IN> BiDiLensSpec<IN, String, String>.int() = this.mapWithNewMeta(String::toInt, Int::toString, NumberParam)
fun <IN> BiDiLensSpec<IN, String, String>.long() = this.mapWithNewMeta(String::toLong, Long::toString, NumberParam)
fun <IN> BiDiLensSpec<IN, String, String>.double() = this.mapWithNewMeta(String::toDouble, Double::toString, NumberParam)
fun <IN> BiDiLensSpec<IN, String, String>.float() = this.mapWithNewMeta(String::toFloat, Float::toString, NumberParam)
fun <IN> BiDiLensSpec<IN, String, String>.boolean() = this.mapWithNewMeta(::safeBooleanFrom, Boolean::toString, BooleanParam)
fun <IN> BiDiLensSpec<IN, String, String>.localDate() = this.map(LocalDate::parse, DateTimeFormatter.ISO_LOCAL_DATE::format)
fun <IN> BiDiLensSpec<IN, String, String>.dateTime() = this.map(LocalDateTime::parse, DateTimeFormatter.ISO_LOCAL_DATE_TIME::format)
fun <IN> BiDiLensSpec<IN, String, String>.zonedDateTime() = this.map(ZonedDateTime::parse, DateTimeFormatter.ISO_ZONED_DATE_TIME::format)
fun <IN> BiDiLensSpec<IN, String, String>.uuid() = this.map(UUID::fromString, java.util.UUID::toString)
fun <IN> BiDiLensSpec<IN, String, String>.regex(pattern: String, group: Int = 1): LensSpec<IN, String, String> {
    val toRegex = pattern.toRegex()
    return this.map { toRegex.matchEntire(it)?.groupValues?.get(group)!! }
}

internal fun safeBooleanFrom(value: String): Boolean =
    if (value.toUpperCase() == "TRUE") true
    else if (value.toUpperCase() == "FALSE") false
    else throw kotlin.IllegalArgumentException("illegal boolean")