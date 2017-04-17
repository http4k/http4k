package org.reekwest.http.core.contract

import org.reekwest.http.core.contract.ContractBreach.Companion.Missing
import org.reekwest.http.then
import java.nio.ByteBuffer
import java.nio.charset.StandardCharsets.UTF_8
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZonedDateTime

interface MultiLensSpec<in IN, OUT : Any> {
    fun optional(name: String, description: String? = null): Lens<IN, OUT, List<OUT?>?>
    fun required(name: String, description: String? = null): Lens<IN, OUT, List<OUT?>>
}

open class LensSpec<IN, OUT : Any>(
    internal val locator: Locator<IN, ByteBuffer>,
    internal val deserialize: (ByteBuffer) -> OUT,
    internal val serialize: (OUT) -> ByteBuffer
) {
    fun <NEXT : Any> map(nextIn: (OUT) -> NEXT): LensSpec<IN, NEXT> = LensSpec(locator,
        deserialize.then(nextIn),
        { UTF_8.encode(it.toString()) })

    fun <NEXT : Any> map(nextIn: (OUT) -> NEXT, nextOut: (NEXT) -> OUT): LensSpec<IN, NEXT> = LensSpec(locator,
        deserialize.then(nextIn),
        nextOut.then(serialize)
    )

    fun optional(name: String, description: String? = null) = object : Lens<IN, OUT, OUT?>(Meta(name, locator.location, false, description), this) {
        override fun convertIn(o: List<OUT?>?) = o?.firstOrNull()
        override fun convertOut(o: OUT?) = o?.let { listOf(it) } ?: emptyList()
    }

    fun required(name: String, description: String? = null) = object : Lens<IN, OUT, OUT>(Meta(name, locator.location, true, description), this) {
        override fun convertIn(o: List<OUT?>?) = o?.firstOrNull() ?: throw Missing(this)
        override fun convertOut(o: OUT) = listOf(o)
    }

    private val spec: LensSpec<IN, OUT> get() = this

    val multi = object : MultiLensSpec<IN, OUT> {
        override fun optional(name: String, description: String?): Lens<IN, OUT, List<OUT?>?> = object : Lens<IN, OUT, List<OUT?>?>(Meta(name, locator.location, false, description), spec) {
            override fun convertIn(o: List<OUT?>?) = o
            override fun convertOut(o: List<OUT?>?) = o?.mapNotNull { it } ?: emptyList()
        }

        override fun required(name: String, description: String?) = object : Lens<IN, OUT, List<OUT?>>(Meta(name, locator.location, true, description), spec) {
            override fun convertIn(o: List<OUT?>?): List<OUT?> {
                val orEmpty = o ?: emptyList()
                return if (orEmpty.isEmpty()) throw Missing(this) else orEmpty
            }

            override fun convertOut(o: List<OUT?>) = o.mapNotNull { it }
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
