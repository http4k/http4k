package org.http4k.format

import org.http4k.asString
import org.http4k.core.Body
import org.http4k.core.ContentType.Companion.APPLICATION_JSON
import org.http4k.lens.BiDiBodyLensSpec
import org.http4k.lens.BiDiLensSpec
import org.http4k.lens.BiDiWsMessageLensSpec
import org.http4k.lens.ContentNegotiation
import org.http4k.lens.ContentNegotiation.Companion.None
import org.http4k.lens.Meta
import org.http4k.lens.ParamMeta.ObjectParam
import org.http4k.lens.httpBodyRoot
import org.http4k.lens.string
import org.http4k.websocket.WsMessage
import java.math.BigDecimal
import java.math.BigInteger

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
    fun bool(value: NODE): Boolean

    fun compactify(s: String) = parse(s).asCompactJsonString()

    fun prettify(s: String) = parse(s).asPrettyJsonString()

    // Utility methods - used when we don't know which implementation we are using
    fun string(value: String): NODE = value.asJsonValue()

    fun number(value: Int): NODE = value.asJsonValue()
    fun number(value: Double): NODE = value.asJsonValue()
    fun number(value: Long): NODE = value.asJsonValue()
    fun number(value: BigDecimal): NODE = value.asJsonValue()
    fun number(value: BigInteger): NODE = value.asJsonValue()
    fun boolean(value: Boolean): NODE = value.asJsonValue()
    fun <T : NODE> array(value: T): ROOT = array(listOf(value))
    fun <T : NODE> array(value: Iterable<T>): ROOT = value.asJsonArray()
    fun obj(): ROOT = obj(emptyList())
    fun <T : NODE> obj(value: Iterable<Pair<String, T>>): ROOT = value.asJsonObject()
    fun <T : NODE> obj(vararg fields: Pair<String, T>): ROOT = obj(fields.asIterable())
    fun nullNode(): NODE {
        val i: Int? = null
        return i.asJsonValue()
    }

    fun parse(s: String): ROOT = s.asJsonObject()
    fun pretty(node: ROOT): String = node.asPrettyJsonString()
    fun compact(node: ROOT): String = node.asCompactJsonString()
    fun <IN> lens(spec: BiDiLensSpec<IN, String>) = spec.map({ parse(it) }, { compact(it) })
    fun <IN> BiDiLensSpec<IN, String>.json() = lens(this)
    fun body(description: String? = null, contentNegotiation: ContentNegotiation = None): BiDiBodyLensSpec<ROOT> =
        httpBodyRoot(listOf(Meta(true, "body", ObjectParam, "body", description)), APPLICATION_JSON, contentNegotiation)
            .map({ it.payload.asString() }, { it: String -> Body(it) })
            .map({ parse(it) }, { compact(it) })

    fun Body.Companion.json(description: String? = null, contentNegotiation: ContentNegotiation = None): BiDiBodyLensSpec<ROOT> = body(description, contentNegotiation)

    fun WsMessage.Companion.json(): BiDiWsMessageLensSpec<ROOT> = WsMessage.string().map({ parse(it) }, { compact(it) })

    fun stringFrom(node: NODE, name: String): String?
}

enum class JsonType {
    Object, Array, String, Integer, Number, Boolean, Null
}