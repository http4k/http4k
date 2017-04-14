package org.reekwest.http.core.contract

import java.nio.ByteBuffer
import java.nio.charset.StandardCharsets.UTF_8
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZonedDateTime

interface MultiLensSpec<in IN, OUT : Any> {
    fun optional(name: String, description: String? = null): Lens<IN, OUT, List<OUT?>?>
    fun required(name: String, description: String? = null): Lens<IN, OUT, List<OUT?>>
}

open class LensSpec<IN, OUT : Any>(private val location: String,
                                   val get: (IN, String) -> List<ByteBuffer?>?,
                                   val set: (IN, String, List<ByteBuffer>) -> IN,
                                   val deserialize: (ByteBuffer) -> OUT,
                                   val serialize: (OUT) -> ByteBuffer
) {
    fun <NEXT : Any> map(nextIn: (OUT) -> NEXT): LensSpec<IN, NEXT> = LensSpec(location,
        get, set,
        deserialize.then(nextIn),
        { UTF_8.encode(it.toString()) })

    fun <NEXT : Any> map(nextIn: (OUT) -> NEXT, nextOut: (NEXT) -> OUT): LensSpec<IN, NEXT> = LensSpec(location,
        get, set,
        deserialize.then(nextIn),
        nextOut.then(serialize)
    )

    fun optional(name: String, description: String? = null) = object : Lens<IN, OUT, OUT?>(Meta(name, location, description), this) {
        override fun convertIn(o: List<OUT?>?): OUT? = o?.firstOrNull()
        override fun convertOut(o: OUT?): List<OUT> = o?.let { listOf(it) } ?: emptyList()
    }

    fun required(name: String, description: String? = null) = object : Lens<IN, OUT, OUT>(Meta(name, location, description), this) {
        override fun convertIn(o: List<OUT?>?): OUT = o?.firstOrNull() ?: throw Missing(meta)
        override fun convertOut(o: OUT): List<OUT> = listOf(o)
    }

    internal val id: LensSpec<IN, OUT>
        get() = this

    val multi = object : MultiLensSpec<IN, OUT> {
        override fun optional(name: String, description: String?): Lens<IN, OUT, List<OUT?>?> = object : Lens<IN, OUT, List<OUT?>?>(Meta(name, location, description), id) {
            override fun convertIn(o: List<OUT?>?) = o
            override fun convertOut(o: List<OUT?>?): List<OUT> = o?.mapNotNull { it } ?: emptyList()
        }

        override fun required(name: String, description: String?) = object : Lens<IN, OUT, List<OUT?>>(Meta(name, location, description), id) {
            override fun convertIn(o: List<OUT?>?): List<OUT?> {
                val orEmpty = o ?: emptyList()
                return if (orEmpty.isEmpty()) throw Missing(meta) else orEmpty
            }

            override fun convertOut(o: List<OUT?>): List<OUT> = o.mapNotNull { it }
        }
    }
}

open class StringLensSpec<IN>(location: String,
                              get: (IN, String) -> List<String?>?,
                              set: (IN, String, List<String>) -> IN)
    : LensSpec<IN, String>(location, { target, name -> get(target, name)?.mapNotNull { it -> it?.toByteBuffer() } },
    { target, name, values -> set(target, name, values.map { String(it.array()) }) },
    { it -> String(it.array()) }, { it.toByteBuffer() }
)
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
