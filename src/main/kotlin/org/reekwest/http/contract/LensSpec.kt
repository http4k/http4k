package org.reekwest.http.contract

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZonedDateTime

interface MultiLensSpec<in IN, OUT> {
    fun optional(name: String, description: String? = null): Lens<IN, OUT, List<OUT>?>
    fun required(name: String, description: String? = null): Lens<IN, OUT, List<OUT>>
}

open class LensSpec<IN, MID, OUT>(internal val location: String, internal val get: Get<IN, MID, OUT>) {
    fun <NEXT> map(nextIn: (OUT) -> NEXT): LensSpec<IN, MID, NEXT> = LensSpec(location, get.map(nextIn))

    open fun optional(name: String, description: String? = null): Lens<IN, OUT, OUT?> =
        Lens(Meta(name, location, false, description), get(name), { it.firstOrNull() })

    open fun required(name: String, description: String? = null): Lens<IN, OUT, OUT> {
        val meta = Meta(name, location, false, description)
        return Lens(meta, get(name), { it.firstOrNull() ?: throw ContractBreach(org.reekwest.http.contract.Missing(meta)) })
    }

    open val multi = object : MultiLensSpec<IN, OUT> {
        override fun optional(name: String, description: String?): Lens<IN, OUT, List<OUT>?> =
            Lens(Meta(name, location, false, description), get(name)
                , { if (it.isEmpty()) null else it })

        override fun required(name: String, description: String?): Lens<IN, OUT, List<OUT>> {
            val meta = Meta(name, location, false, description)
            return Lens(meta, get(name),
                { if (it.isEmpty()) throw ContractBreach(org.reekwest.http.contract.Missing(meta)) else it })
        }
    }
}

interface BiDiMultiLensSpec<in IN, OUT> : MultiLensSpec<IN, OUT> {
    override fun optional(name: String, description: String?): BiDiLens<IN, OUT, List<OUT>?>
    override fun required(name: String, description: String?): BiDiLens<IN, OUT, List<OUT>>
}

open class BiDiLensSpec<IN, MID, OUT>(location: String, get: Get<IN, MID, OUT>,
                                      private val set: Set<IN, MID, OUT>) : LensSpec<IN, MID, OUT>(location, get) {

    fun <NEXT> map(nextIn: (OUT) -> NEXT, nextOut: (NEXT) -> OUT): BiDiLensSpec<IN, MID, NEXT> =
        BiDiLensSpec(location, get.map(nextIn), set.map(nextOut))

    override fun optional(name: String, description: String?): BiDiLens<IN, OUT, OUT?> =
        BiDiLens(Meta(name, location, false, description), get(name),
            { it.firstOrNull() },
            set(name),
            { it?.let { listOf(it) } ?: emptyList() })

    override fun required(name: String, description: String?): BiDiLens<IN, OUT, OUT> {
        val meta = Meta(name, location, true, description)
        return BiDiLens(meta, get(name),
            { it.firstOrNull() ?: throw ContractBreach(org.reekwest.http.contract.Missing(meta)) },
            set(name), { listOf(it) })
    }

    override val multi = object : BiDiMultiLensSpec<IN, OUT> {
        override fun optional(name: String, description: String?): BiDiLens<IN, OUT, List<OUT>?> =
            BiDiLens(Meta(name, location, false, description), get(name),
                { if (it.isEmpty()) null else it },
                set(name),
                { it ?: emptyList() }
            )

        override fun required(name: String, description: String?): BiDiLens<IN, OUT, List<OUT>> {
            val meta = Meta(name, location, true, description)
            return BiDiLens(meta, get(name),
                { if (it.isEmpty()) throw ContractBreach(org.reekwest.http.contract.Missing(meta)) else it },
                set(name), { it })
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