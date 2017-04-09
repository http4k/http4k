package org.reekwest.http.core.contract

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZonedDateTime

interface MultiSpec<in IN, OUT> {
    fun optional(name: String, description: String? = null): MsgPart<IN, OUT, List<OUT?>?>
    fun required(name: String, description: String? = null): MsgPart<IN, OUT, List<OUT?>>
}

open class Spec<in IN, OUT>(private val location: String, val fn: (IN, String) -> List<OUT?>?) {
    fun <NEXT> map(next: (OUT) -> NEXT): Spec<IN, NEXT> = Spec(location)
    { req, name -> fn(req, name)?.let { it.map { it?.let(next) } } }

    fun optional(name: String, description: String? = null) = object : MsgPart<IN, OUT, OUT?>(Meta(name, location, description), this) {
        override fun convert(o: List<OUT?>?): OUT? = o?.firstOrNull()
    }

    fun required(name: String, description: String? = null) = object : MsgPart<IN, OUT, OUT>(Meta(name, location, description), this) {
        override fun convert(o: List<OUT?>?): OUT = o?.firstOrNull() ?: throw Missing(meta)
    }

    internal val id: Spec<IN, OUT>
        get() = this

    val multi = object : MultiSpec<IN, OUT> {
        override fun optional(name: String, description: String?): MsgPart<IN, OUT, List<OUT?>?> = object : MsgPart<IN, OUT, List<OUT?>?>(Meta(name, location, description), id) {
            override fun convert(o: List<OUT?>?) = o
        }

        override fun required(name: String, description: String?) = object : MsgPart<IN, OUT, List<OUT?>>(Meta(name, location, description), id) {
            override fun convert(o: List<OUT?>?): List<OUT?> {
                val orEmpty = o ?: emptyList()
                return if (orEmpty.isEmpty()) throw Missing(meta) else orEmpty
            }
        }
    }
}

// Extension methods for commonly used conversions

fun <IN> Spec<IN, String>.int(): Spec<IN, Int> = this.map(String::toInt)
fun <IN> Spec<IN, String>.long(): Spec<IN, Long> = this.map(String::toLong)
fun <IN> Spec<IN, String>.double(): Spec<IN, Double> = this.map(String::toDouble)
fun <IN> Spec<IN, String>.float(): Spec<IN, Float> = this.map(String::toFloat)

fun <IN> Spec<IN, String>.boolean(): Spec<IN, Boolean> = this.map {
    if (it.toUpperCase() == "TRUE") true
    else if (it.toUpperCase() == "FALSE") false
    else throw kotlin.IllegalArgumentException("illegal boolean")
}

fun <IN> Spec<IN, String>.localDate(): Spec<IN, LocalDate> = this.map { LocalDate.parse(it) }
fun <IN> Spec<IN, String>.dateTime(): Spec<IN, LocalDateTime> = this.map { LocalDateTime.parse(it) }
fun <IN> Spec<IN, String>.zonedDateTime(): Spec<IN, ZonedDateTime> = this.map { ZonedDateTime.parse(it) }
