package org.reekwest.http.core.contract

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZonedDateTime

interface MultiLensBuilder<in IN, OUT> {
    fun optional(name: String, description: String? = null): Lens<IN, OUT, List<OUT?>?>
    fun required(name: String, description: String? = null): Lens<IN, OUT, List<OUT?>>
}

open class LensBuilder<in IN, OUT>(private val location: String, val fn: (IN, String) -> List<OUT?>?) {
    fun <NEXT> map(next: (OUT) -> NEXT): LensBuilder<IN, NEXT> = LensBuilder(location)
    { req, name -> fn(req, name)?.let { it.map { it?.let(next) } } }

    fun optional(name: String, description: String? = null) = object : Lens<IN, OUT, OUT?>(Meta(name, location, description), this) {
        override fun convert(o: List<OUT?>?): OUT? = o?.firstOrNull()
    }

    fun required(name: String, description: String? = null) = object : Lens<IN, OUT, OUT>(Meta(name, location, description), this) {
        override fun convert(o: List<OUT?>?): OUT = o?.firstOrNull() ?: throw Missing(meta)
    }

    internal val id: LensBuilder<IN, OUT>
        get() = this

    val multi = object : MultiLensBuilder<IN, OUT> {
        override fun optional(name: String, description: String?): Lens<IN, OUT, List<OUT?>?> = object : Lens<IN, OUT, List<OUT?>?>(Meta(name, location, description), id) {
            override fun convert(o: List<OUT?>?) = o
        }

        override fun required(name: String, description: String?) = object : Lens<IN, OUT, List<OUT?>>(Meta(name, location, description), id) {
            override fun convert(o: List<OUT?>?): List<OUT?> {
                val orEmpty = o ?: emptyList()
                return if (orEmpty.isEmpty()) throw Missing(meta) else orEmpty
            }
        }
    }
}

// Extension methods for commonly used conversions

fun <IN> LensBuilder<IN, String>.int(): LensBuilder<IN, Int> = this.map(String::toInt)
fun <IN> LensBuilder<IN, String>.long(): LensBuilder<IN, Long> = this.map(String::toLong)
fun <IN> LensBuilder<IN, String>.double(): LensBuilder<IN, Double> = this.map(String::toDouble)
fun <IN> LensBuilder<IN, String>.float(): LensBuilder<IN, Float> = this.map(String::toFloat)

fun <IN> LensBuilder<IN, String>.boolean(): LensBuilder<IN, Boolean> = this.map {
    if (it.toUpperCase() == "TRUE") true
    else if (it.toUpperCase() == "FALSE") false
    else throw kotlin.IllegalArgumentException("illegal boolean")
}

fun <IN> LensBuilder<IN, String>.localDate(): LensBuilder<IN, LocalDate> = this.map { LocalDate.parse(it) }
fun <IN> LensBuilder<IN, String>.dateTime(): LensBuilder<IN, LocalDateTime> = this.map { LocalDateTime.parse(it) }
fun <IN> LensBuilder<IN, String>.zonedDateTime(): LensBuilder<IN, ZonedDateTime> = this.map { ZonedDateTime.parse(it) }
