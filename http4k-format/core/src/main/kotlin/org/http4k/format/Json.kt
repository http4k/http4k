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
interface Json<NODE> {
    // Contract methods to be implemented
    fun NODE.asPrettyJsonString(): String
    fun NODE.asCompactJsonString(): String
    fun String.asJsonObject(): NODE
    fun String?.asJsonValue(): NODE
    fun Int?.asJsonValue(): NODE
    fun Double?.asJsonValue(): NODE
    fun Long?.asJsonValue(): NODE
    fun BigDecimal?.asJsonValue(): NODE
    fun BigInteger?.asJsonValue(): NODE
    fun Boolean?.asJsonValue(): NODE
    fun <T : Iterable<NODE>> T.asJsonArray(): NODE
    fun <LIST : Iterable<Pair<String, NODE>>> LIST.asJsonObject(): NODE
    fun typeOf(value: NODE): JsonType
    fun fields(node: NODE): Iterable<Pair<String, NODE>>
    fun elements(value: NODE): Iterable<NODE>
    fun text(value: NODE): String
    fun bool(value: NODE): Boolean
    fun integer(value: NODE): Long
    fun decimal(value: NODE): BigDecimal

    fun compactify(input: String) = parse(input).asCompactJsonString()

    fun prettify(input: String) = parse(input).asPrettyJsonString()

    // Utility methods - used when we don't know which implementation we are using
    fun string(value: String): NODE = value.asJsonValue()

    fun number(value: Int): NODE = value.asJsonValue()
    fun number(value: Double): NODE = value.asJsonValue()
    fun number(value: Long): NODE = value.asJsonValue()
    fun number(value: BigDecimal): NODE = value.asJsonValue()
    fun number(value: BigInteger): NODE = value.asJsonValue()
    fun boolean(value: Boolean): NODE = value.asJsonValue()
    fun <T : NODE> array(value: T): NODE = array(listOf(value))
    fun <T : NODE> array(value: Iterable<T>): NODE = value.asJsonArray()
    fun obj(): NODE = obj<NODE>(emptyList())
    fun <T : NODE> obj(value: Iterable<Pair<String, T>>): NODE = value.asJsonObject()
    fun <T : NODE> obj(vararg fields: Pair<String, T>): NODE = obj(fields.asIterable())
    fun nullNode(): NODE {
        val i: Int? = null
        return i.asJsonValue()
    }

    fun parse(input: String): NODE = input.asJsonObject()
    fun pretty(node: NODE): String = node.asPrettyJsonString()
    fun compact(node: NODE): String = node.asCompactJsonString()
    fun <IN : Any> lens(spec: BiDiLensSpec<IN, String>) = spec.map(::parse, this::compact)
    fun <IN : Any> BiDiLensSpec<IN, String>.json() = lens(this)
    fun body(description: String? = null, contentNegotiation: ContentNegotiation = None): BiDiBodyLensSpec<NODE> =
        httpBodyRoot(listOf(Meta(true, "body", ObjectParam, "body", description)), APPLICATION_JSON, contentNegotiation)
            .map({ it.payload.asString() }, { Body(it) })
            .map({ parse(it) }, { compact(it) })

    fun Body.Companion.json(description: String? = null, contentNegotiation: ContentNegotiation = None): BiDiBodyLensSpec<NODE> = body(description, contentNegotiation)

    fun WsMessage.Companion.json(): BiDiWsMessageLensSpec<NODE> = WsMessage.string().map({ parse(it) }, { compact(it) })

    fun textValueOf(node: NODE, name: String): String?

    operator fun <T> invoke(fn: Json<NODE>.() -> T): T = run(fn)
}

enum class JsonType {
    Object, Array, String, Integer, Number, Boolean, Null
}
