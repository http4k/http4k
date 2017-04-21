package org.reekwest.http.contract

import org.reekwest.http.contract.ContractBreach.Companion.Missing
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZonedDateTime

interface MultiGetLensSpec<in IN, OUT> {
    fun optional(name: String, description: String? = null): MetaLens<IN, OUT, List<OUT>?>
    fun required(name: String, description: String? = null): MetaLens<IN, OUT, List<OUT>>
}

open class GetLensSpec<IN, MID, OUT>(internal val location: String, internal val createGetLens: GetLens<IN, MID, OUT>) {
    fun <NEXT> map(nextIn: (OUT) -> NEXT): GetLensSpec<IN, MID, NEXT> = GetLensSpec(location, createGetLens.map(nextIn))

    open fun optional(name: String, description: String? = null): MetaLens<IN, OUT, OUT?> =
        object : MetaLens<IN, OUT, OUT?>(Meta(name, location, false, description), createGetLens(name)) {
            override fun convertIn(o: List<OUT>): OUT? = o.firstOrNull()
        }

    open fun required(name: String, description: String? = null): MetaLens<IN, OUT, OUT> =
        object : MetaLens<IN, OUT, OUT>(Meta(name, location, false, description), createGetLens(name)) {
            override fun convertIn(o: List<OUT>): OUT = o.firstOrNull() ?: throw Missing(this)
        }

    open val multi = object : MultiGetLensSpec<IN, OUT> {
        override fun optional(name: String, description: String?): MetaLens<IN, OUT, List<OUT>?> =
            object : MetaLens<IN, OUT, List<OUT>?>(Meta(name, location, false, description), createGetLens(name)) {
                override fun convertIn(o: List<OUT>): List<OUT>? = if (o.isEmpty()) null else o
            }

        override fun required(name: String, description: String?): MetaLens<IN, OUT, List<OUT>> =
            object : MetaLens<IN, OUT, List<OUT>>(Meta(name, location, false, description), createGetLens(name)) {
                override fun convertIn(o: List<OUT>): List<OUT> = if (o.isEmpty()) throw Missing(this) else o
            }
    }
}

interface BiDiMultiLensSpec<IN, OUT> : MultiGetLensSpec<IN, OUT> {
    override fun optional(name: String, description: String?): BiDiMetaLens<IN, OUT, List<OUT>?>
    override fun required(name: String, description: String?): BiDiMetaLens<IN, OUT, List<OUT>>
}

open class BiDiLensSpec<IN, MID, OUT>(location: String, createGetLens: GetLens<IN, MID, OUT>,
                                      private val createSetLens: SetLens<IN, MID, OUT>) : GetLensSpec<IN, MID, OUT>(location, createGetLens) {

    private fun biDiLensFor(name: String): BiDiLens<IN, OUT> = BiDiLens(createGetLens(name), createSetLens(name))

    fun <NEXT> map(nextIn: (OUT) -> NEXT, nextOut: (NEXT) -> OUT): BiDiLensSpec<IN, MID, NEXT> =
        BiDiLensSpec(location,
            createGetLens.map(nextIn),
            createSetLens.map(nextOut))

    override fun optional(name: String, description: String?): BiDiMetaLens<IN, OUT, OUT?> =
        object : BiDiMetaLens<IN, OUT, OUT?>(Meta(name, location, false, description), biDiLensFor(name)) {
            override fun convertIn(o: List<OUT>): OUT? = o.firstOrNull()
            override fun convertOut(o: OUT?): List<OUT> = o?.let { listOf(it) } ?: emptyList()
        }

    override fun required(name: String, description: String?): BiDiMetaLens<IN, OUT, OUT> =
        object : BiDiMetaLens<IN, OUT, OUT>(Meta(name, location, true, description), biDiLensFor(name)) {
            override fun convertIn(o: List<OUT>): OUT = o.firstOrNull() ?: throw Missing(this)
            override fun convertOut(o: OUT): List<OUT> = listOf(o)
        }

    override val multi = object : BiDiMultiLensSpec<IN, OUT> {
        override fun optional(name: String, description: String?): BiDiMetaLens<IN, OUT, List<OUT>?> =
            object : BiDiMetaLens<IN, OUT, List<OUT>?>(Meta(name, location, false, description), biDiLensFor(name)) {
                override fun convertOut(o: List<OUT>?): List<OUT> = o ?: emptyList()
                override fun convertIn(o: List<OUT>): List<OUT>? = if (o.isEmpty()) null else o
            }

        override fun required(name: String, description: String?): BiDiMetaLens<IN, OUT, List<OUT>> =
            object : BiDiMetaLens<IN, OUT, List<OUT>>(Meta(name, location, true, description), biDiLensFor(name)) {
                override fun convertOut(o: List<OUT>): List<OUT> = o
                override fun convertIn(o: List<OUT>): List<OUT> = if (o.isEmpty()) throw Missing(this) else o
            }
    }
}

fun <IN> BiDiLensSpec<IN, String, String>.int() = this.map(String::toInt, Int::toString)
fun <IN> BiDiLensSpec<IN, String, String>.long() = this.map(String::toLong, Long::toString)
fun <IN> BiDiLensSpec<IN, String, String>.double() = this.map(String::toDouble, Double::toString)
fun <IN> BiDiLensSpec<IN, String, String>.float() = this.map(String::toFloat, Float::toString)

fun <IN> BiDiLensSpec<IN, String, String>.boolean() = this.map({
    if (it.toUpperCase() == "TRUE") true
    else if (it.toUpperCase() == "FALSE") false
    else throw kotlin.IllegalArgumentException("illegal boolean")
}, Boolean::toString)

fun <IN> BiDiLensSpec<IN, String, String>.localDate() = this.map { LocalDate.parse(it) }
fun <IN> BiDiLensSpec<IN, String, String>.dateTime() = this.map { LocalDateTime.parse(it) }
fun <IN> BiDiLensSpec<IN, String, String>.zonedDateTime() = this.map { ZonedDateTime.parse(it) }