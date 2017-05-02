package org.reekwest.http.formats

import org.reekwest.http.lens.BiDiBodySpec
import org.reekwest.http.lens.BiDiLensSpec
import org.reekwest.http.lens.Body
import java.math.BigDecimal
import java.math.BigInteger
import java.nio.ByteBuffer

/**
 * This is the contract for all JSON implementations
 */
interface Json<ROOT : NODE, NODE> {
    // Contract methods to be implemented
    fun ROOT.asPrettyJsonString(): String
    fun ROOT.asCompactJsonString(): String
    fun String.asJsonObject(): ROOT
    fun String?.asJsonValue(): NODE
    fun Int?.asJsonValue(): NODE
    fun Double?.asJsonValue(): NODE
    fun Long?.asJsonValue(): NODE
    fun BigDecimal?.asJsonValue(): NODE
    fun BigInteger?.asJsonValue(): NODE
    fun Boolean?.asJsonValue(): NODE
    fun <T : Iterable<NODE>> T.asJsonArray(): ROOT
    fun <LIST : Iterable<Pair<String, NODE>>> LIST.asJsonObject(): ROOT
    fun typeOf(value: NODE): JsonType
    fun fields(node: NODE): Iterable<Pair<String, NODE>>
    fun elements(value: NODE): Iterable<NODE>
    fun text(value: NODE): String

    // Utility methods - used when we don't know which implementation we are using
    fun string(value: String): NODE = value.asJsonValue()
    fun number(value: Int): NODE = value.asJsonValue()
    fun number(value: Double): NODE = value.asJsonValue()
    fun number(value: Long): NODE = value.asJsonValue()
    fun number(value: BigDecimal): NODE = value.asJsonValue()
    fun number(value: BigInteger): NODE = value.asJsonValue()
    fun boolean(value: Boolean): NODE = value.asJsonValue()
    fun <T : NODE> array(value: Iterable<T>): ROOT = value.asJsonArray()
    fun <T : NODE> obj(value: Iterable<Pair<String, T>>): ROOT = value.asJsonObject()
    fun <T : NODE> obj(vararg fields: Pair<String, T>): ROOT = obj(fields.asIterable())
    fun nullNode(): NODE {
        val i: Int? = null
        return i.asJsonValue()
    }
    fun parse(s: String): ROOT = s.asJsonObject()
    fun pretty(node: ROOT): String = node.asPrettyJsonString()
    fun compact(node: ROOT): String = node.asCompactJsonString()
    fun <IN> lens(spec: BiDiLensSpec<IN, String, String>) = spec.map({ parse(it) }, { compact(it) })
    fun <IN> BiDiLensSpec<IN, String, String>.json() = lens(this)
    fun body(): BiDiBodySpec<ByteBuffer, ROOT> = Body.string.map({ parse(it) }, { compact(it) })
    fun Body.json(): BiDiBodySpec<ByteBuffer, ROOT> = body()
}

enum class JsonType {
    Object, Array, String, Number, Boolean, Null
}