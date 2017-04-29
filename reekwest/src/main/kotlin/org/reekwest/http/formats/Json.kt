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
    fun ROOT.asPrettyJsonString(): String
    fun ROOT.asCompactJsonString(): String
    fun String.fromJsonString(): ROOT
    fun String?.asJson(): NODE
    fun Int?.asJson(): NODE
    fun Double?.asJson(): NODE
    fun Long?.asJson(): NODE
    fun BigDecimal?.asJson(): NODE
    fun BigInteger?.asJson(): NODE
    fun Boolean?.asJson(): NODE
    fun <T : Iterable<NODE>> T.asJsonArray(): ROOT
    fun <LIST : Iterable<Pair<String, NODE>>> LIST.asJsonObject(): ROOT

    fun <IN> BiDiLensSpec<IN, String, String>.json(): BiDiLensSpec<IN, String, ROOT>
    fun Body.json(): BiDiBodySpec<ByteBuffer, ROOT>

    // TODO work out which ones of these we want to keep
    fun obj(fields: Iterable<Pair<String, NODE>>): ROOT = fields.asJsonObject()
    fun obj(vararg fields: Pair<String, NODE>): ROOT = obj(fields.asIterable())
    fun parse(s: String): ROOT = s.fromJsonString()
    fun pretty(node: ROOT): String = node.asPrettyJsonString()
    fun compact(node: ROOT): String = node.asCompactJsonString()
}
