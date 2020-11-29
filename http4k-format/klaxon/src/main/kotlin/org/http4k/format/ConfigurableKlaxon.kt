package org.http4k.format

import com.beust.klaxon.JsonObject
import com.beust.klaxon.JsonParsingException
import org.http4k.core.Body
import org.http4k.core.ContentType
import org.http4k.core.ContentType.Companion.APPLICATION_JSON
import org.http4k.lens.BiDiBodyLensSpec
import org.http4k.lens.ContentNegotiation
import org.http4k.lens.ContentNegotiation.Companion.None
import org.http4k.websocket.WsMessage
import java.math.BigDecimal
import java.math.BigInteger
import kotlin.reflect.KClass
import com.beust.klaxon.Klaxon as KKlaxon

open class ConfigurableKlaxon(val klaxon: KKlaxon,
                              val defaultContentType: ContentType = APPLICATION_JSON) : JsonLibAutoMarshallingJson<JsonObject>() {

    override fun typeOf(value: JsonObject): JsonType = when (value) {
//        is TextNode -> JsonType.String
//        is BooleanNode -> JsonType.Boolean
//        is NumericNode -> JsonType.Number
//        is ArrayNode -> JsonType.Array
//        is ObjectNode -> JsonType.Object
//        is NullNode -> JsonType.Null
        else -> throw IllegalArgumentException("Don't know now to translate $value")
    }

    override fun String.asJsonObject(): JsonObject = klaxon.parse(this) ?: throw JsonParsingException("")
    override fun String?.asJsonValue(): JsonObject = asJsonObject()
    override fun Int?.asJsonValue(): JsonObject = asJsonObject()
    override fun Double?.asJsonValue(): JsonObject = asJsonObject()
    override fun Long?.asJsonValue(): JsonObject = asJsonObject()
    override fun BigDecimal?.asJsonValue(): JsonObject = asJsonObject()
    override fun BigInteger?.asJsonValue(): JsonObject = asJsonObject()
    override fun Boolean?.asJsonValue(): JsonObject = asJsonObject()
    override fun <T : Iterable<JsonObject>> T.asJsonArray(): JsonObject = TODO()
    override fun JsonObject.asPrettyJsonString(): String = TODO()
    override fun JsonObject.asCompactJsonString(): String = TODO()
    override fun <LIST : Iterable<Pair<String, JsonObject>>> LIST.asJsonObject(): JsonObject =
        TODO()

    override fun fields(node: JsonObject) = TODO()

    override fun elements(value: JsonObject) = TODO()
    override fun text(value: JsonObject): String = TODO()
    override fun bool(value: JsonObject): Boolean = TODO()
    override fun integer(value: JsonObject) = TODO()
    override fun decimal(value: JsonObject) = TODO()
    override fun textValueOf(node: JsonObject, name: String) = TODO()

    // auto
    override fun asJsonObject(input: Any): JsonObject = TODO()

    override fun <T : Any> asA(input: String, target: KClass<T>): T = TODO()
    override fun <T : Any> asA(j: JsonObject, target: KClass<T>): T = TODO()

    inline fun <reified T : Any> JsonObject.asA(): T = TODO()

    inline fun <reified T : Any> WsMessage.Companion.auto(): BiDiBodyLensSpec<T> = TODO()

    inline fun <reified T : Any> Body.Companion.auto(description: String? = null,
                                                     contentNegotiation: ContentNegotiation = None,
                                                     contentType: ContentType = defaultContentType) = autoBody<T>(description, contentNegotiation, contentType)

    inline fun <reified T : Any> autoBody(description: String? = null,
                                          contentNegotiation: ContentNegotiation = None,
                                          contentType: ContentType = defaultContentType)
        : BiDiBodyLensSpec<T> = httpBodyLens(description, contentNegotiation, contentType)
        .map({ klaxon.parse(it)!! }, klaxon::toJsonString)

}

fun KKlaxon.asConfigurable() = asConfigurable(KKlaxon())

