package org.http4k.format

import com.squareup.moshi.JsonReader
import com.squareup.moshi.JsonWriter
import okio.Buffer
import okio.buffer
import okio.source
import java.math.BigDecimal
import java.math.BigInteger

object MoshiJson: Json<MoshiElement> {

    override fun MoshiElement.asPrettyJsonString(): String {
        val buffer = Buffer()
        JsonWriter.of(buffer).use { writer ->
            writer.indent = "  "
            write(writer)
        }
        return buffer.readUtf8()
    }

    override fun MoshiElement.asCompactJsonString(): String {
        val buffer = Buffer()
        JsonWriter.of(buffer).use { writer ->
            write(writer)
        }
        return buffer.readUtf8()
    }

    private fun MoshiElement.write(writer: JsonWriter) {
        when(this) {
            is MoshiArray -> writeArray(writer)
            is MoshiObject -> writeObject(writer)
            is MoshiPrimitive -> writePrimitive(writer)
        }
    }

    private fun MoshiArray.writeArray(writer: JsonWriter) {
        writer.beginArray()
        for (element in elements) {
            element.write(writer)
        }
        writer.endArray()
    }

    private fun MoshiObject.writeObject(writer: JsonWriter) {
        writer.beginObject()
        for ((name, value) in attributes) {
            writer.name(name)
            value.write(writer)
        }
        writer.endObject()
    }

    private fun MoshiPrimitive.writePrimitive(writer: JsonWriter) {
        when(this) {
            is MoshiString -> writer.value(value)
            is MoshiNumber -> writer.value(value)
            is MoshiNull -> writer.nullValue()
            is MoshiBoolean -> writer.value(value)
        }
    }

    override fun String.asJsonObject(): MoshiElement {
        JsonReader.of(byteInputStream().source().buffer()).use { reader ->
            return reader.parse()
        }
    }

    private fun JsonReader.parse(): MoshiElement {
        return when(val token = peek()) {
            JsonReader.Token.BEGIN_ARRAY -> parseArray()
            JsonReader.Token.BEGIN_OBJECT -> parseObject()
            JsonReader.Token.STRING -> MoshiString(nextString())
            JsonReader.Token.BOOLEAN -> MoshiBoolean(nextBoolean())
            JsonReader.Token.NUMBER -> MoshiNumber(nextDouble())
            JsonReader.Token.NULL -> { nextNull<Any>(); MoshiNull }
            else -> throw java.lang.IllegalStateException("can't handle $token")
        }
    }

    private fun JsonReader.parseObject(): MoshiObject {
        val attributes = mutableMapOf<String, MoshiElement>()

        beginObject()
        while(hasNext()) {
            val name = nextName()
            val value = parse()
            attributes[name] = value
        }
        endObject()
        return MoshiObject(attributes)
    }

    private fun JsonReader.parseArray(): MoshiArray {
        val elements = mutableListOf<MoshiElement>()

        beginArray()
        while(hasNext()) {
            elements += parse()
        }
        endArray()
        return MoshiArray(elements)
    }


    override fun <LIST : Iterable<Pair<String, MoshiElement>>> LIST.asJsonObject() = MoshiObject(toMap())
    override fun String?.asJsonValue() = if (this == null) MoshiNull else MoshiString(this)
    override fun Int?.asJsonValue() = if (this == null) MoshiNull else MoshiNumber(this)
    override fun Double?.asJsonValue() = if (this == null) MoshiNull else MoshiNumber(this)
    override fun Long?.asJsonValue() = if (this == null) MoshiNull else MoshiNumber(this)
    override fun BigDecimal?.asJsonValue() = if (this == null) MoshiNull else MoshiNumber(this)
    override fun BigInteger?.asJsonValue() = if (this == null) MoshiNull else MoshiNumber(this)
    override fun Boolean?.asJsonValue() = if (this == null) MoshiNull else MoshiBoolean(this)
    override fun <T : Iterable<MoshiElement>> T.asJsonArray() = MoshiArray(toList())

    override fun textValueOf(node: MoshiElement, name: String) = (node as MoshiObject)
        .attributes[name]
        ?.let { text(it) }

    override fun decimal(value: MoshiElement) = when(val num = (value as MoshiNumber).value) {
        is Long -> num.toBigDecimal()
        is Int -> num.toBigDecimal()
        is Float -> num.toBigDecimal()
        is Double -> num.toBigDecimal()
        is BigDecimal -> num
        is BigInteger -> num.toBigDecimal()
        else -> throw java.lang.IllegalArgumentException("Cannot convert $value to BigDecimal")
    }

    override fun integer(value: MoshiElement) = ((value as MoshiNumber).value).toLong()
    override fun bool(value: MoshiElement) = (value as MoshiBoolean).value
    override fun text(value: MoshiElement) = (value as MoshiString).value
    override fun elements(value: MoshiElement) = (value as MoshiArray).elements
    override fun fields(node: MoshiElement) = (node as MoshiObject).attributes.map { it.key to it.value }

    override fun typeOf(value: MoshiElement) = when(value) {
        is MoshiObject -> JsonType.Object
        is MoshiArray -> JsonType.Array
        is MoshiNull -> JsonType.Null
        is MoshiNumber -> JsonType.Number
        is MoshiString -> JsonType.String
        is MoshiBoolean -> JsonType.Boolean
    }
}
