package org.reekwest.http.contract

import org.reekwest.http.contract.ContractBreach.Companion.Missing
import java.nio.ByteBuffer
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZonedDateTime

interface MultiLensSpec<IN, OUT> {
    fun optional(name: String, description: String? = null): BiDiContractualLens<IN, OUT, List<OUT>?>
    fun required(name: String, description: String? = null): BiDiContractualLens<IN, OUT, List<OUT>>
}

open class LensSpec<IN, OUT>(
    private val location: String,
    private val createLens: (String) -> Lens<IN, ByteBuffer>,
    private val mapper: BiDiMapper<ByteBuffer, OUT>
) {
    private fun mappingLens(name: String): Lens<IN, OUT> {
        val lens = createLens(name)

        return object : Lens<IN, OUT> {
            override fun invoke(target: IN): List<OUT> = lens(target).let { it.map { it.let { mapper.mapIn(it) } } }
            override fun invoke(values: List<OUT>, target: IN): IN = lens(values.map { mapper.mapOut(it) }, target)
        }
    }

    fun <NEXT> map(nextIn: (OUT) -> NEXT): LensSpec<IN, NEXT> = LensSpec(location, createLens, mapper.map(nextIn))

    fun <NEXT> map(nextIn: (OUT) -> NEXT, nextOut: (NEXT) -> OUT): LensSpec<IN, NEXT> = LensSpec(location, createLens, mapper.map(nextIn, nextOut))

    /**
     * Create a lens which resolves to a single optional (nullable) value
     */
    fun optional(name: String, description: String? = null) =
        object : BiDiContractualLens<IN, OUT, OUT?>(Meta(name, location, false, description), mappingLens(name)) {
            override fun convertOut(o: OUT?) = o?.let { listOf(it) } ?: emptyList()
            override fun convertIn(o: List<OUT>) = o.firstOrNull()
        }

    /**
     * Create a lens which resolves to a single required (non-nullable) value.
     */
    fun required(name: String, description: String? = null) = object : BiDiContractualLens<IN, OUT, OUT>(Meta(name, location, true, description), mappingLens(name)) {
        override fun convertIn(o: List<OUT>) = o.firstOrNull() ?: throw Missing(this)
        override fun convertOut(o: OUT) = listOf(o)
    }

    val multi = object : MultiLensSpec<IN, OUT> {

        /**
         * Create a lens which resolves to a optional (nullable) list value
         */
        override fun optional(name: String, description: String?): BiDiContractualLens<IN, OUT, List<OUT>?> = object : BiDiContractualLens<IN, OUT, List<OUT>?>(Meta(name, location, false, description), mappingLens(name)) {
            override fun convertIn(o: List<OUT>): List<OUT>? = if (o.isEmpty()) null else o
            override fun convertOut(o: List<OUT>?): List<OUT> = o ?: emptyList()
        }

        /**
         * Create a lens which resolves to required (non-nullable) list value.
         */
        override fun required(name: String, description: String?) = object : BiDiContractualLens<IN, OUT, List<OUT>>(Meta(name, location, true, description), mappingLens(name)) {
            override fun convertOut(o: List<OUT>): List<OUT> = o
            override fun convertIn(o: List<OUT>): List<OUT> = if (o.isEmpty()) throw Missing(this) else o
        }
    }
}

// Extension methods for commonly used conversions

fun <IN> LensSpec<IN, String>.int(): LensSpec<IN, Int> = this.map(String::toInt)
fun <IN> LensSpec<IN, String>.long(): LensSpec<IN, Long> = this.map(String::toLong)
fun <IN> LensSpec<IN, String>.double(): LensSpec<IN, Double> = this.map(String::toDouble)
fun <IN> LensSpec<IN, String>.float(): LensSpec<IN, Float> = this.map(String::toFloat)

fun <IN> LensSpec<IN, String>.boolean(): LensSpec<IN, Boolean> = this.map {
    if (it.toUpperCase() == "TRUE") true
    else if (it.toUpperCase() == "FALSE") false
    else throw kotlin.IllegalArgumentException("illegal boolean")
}

fun <IN> LensSpec<IN, String>.localDate(): LensSpec<IN, LocalDate> = this.map { LocalDate.parse(it) }
fun <IN> LensSpec<IN, String>.dateTime(): LensSpec<IN, LocalDateTime> = this.map { LocalDateTime.parse(it) }
fun <IN> LensSpec<IN, String>.zonedDateTime(): LensSpec<IN, ZonedDateTime> = this.map { ZonedDateTime.parse(it) }
