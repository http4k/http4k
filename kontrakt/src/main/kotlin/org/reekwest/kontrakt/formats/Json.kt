package org.reekwest.kontrakt.formats

import org.reekwest.kontrakt.BiDiBodySpec
import org.reekwest.kontrakt.Body
import org.reekwest.kontrakt.lens.BiDiLensSpec
import java.math.BigDecimal
import java.math.BigInteger
import java.nio.ByteBuffer

interface Json<ROOT : NODE, NODE> {
    fun String.fromJson(): ROOT
    fun String?.asJson(): NODE
    fun Int?.asJson(): NODE
    fun Double?.asJson(): NODE
    fun Long?.asJson(): NODE
    fun BigDecimal?.asJson(): NODE
    fun BigInteger?.asJson(): NODE
    fun Boolean?.asJson(): NODE
    fun ROOT.asPretty(): String
    fun ROOT.asCompact(): String
    fun <T : Iterable<NODE>> T.asJsonArray(): ROOT
    fun <LIST : Iterable<Pair<String, NODE>>> LIST.asJson(): ROOT

    fun <IN> BiDiLensSpec<IN, String, String>.json(): BiDiLensSpec<IN, String, ROOT>
    fun Body.json(): BiDiBodySpec<ByteBuffer, ROOT>

    // TODO work out which ones of these we want to keep
    fun obj(fields: Iterable<Pair<String, NODE>>): ROOT = fields.asJson()

    fun obj(vararg fields: Pair<String, NODE>): ROOT = obj(fields.asIterable())
    fun parse(s: String): ROOT = s.fromJson()
    fun pretty(node: ROOT): String = node.asPretty()
    fun compact(node: ROOT): String = node.asCompact()
}
