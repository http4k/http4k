package org.http4k.format

import com.squareup.moshi.JsonReader
import com.squareup.moshi.JsonWriter
import okio.Buffer
import okio.buffer
import okio.source

sealed interface MoshiElement {
    companion object
}

data class MoshiArray(val elements: List<MoshiElement>): MoshiElement
data class MoshiObject(val attributes: Map<String, MoshiElement>): MoshiElement
sealed interface MoshiPrimitive: MoshiElement
data class MoshiString(val value: String): MoshiPrimitive
data class MoshiNumber(val value: Number): MoshiPrimitive
data class MoshiBoolean(val value: Boolean): MoshiPrimitive
object MoshiNull: MoshiPrimitive

fun MoshiElement.toJson(configure: (JsonWriter) -> Unit = {}): String {
    val buffer = Buffer()
    JsonWriter.of(buffer).use { writer ->
        configure(writer)
        write(writer)
    }
    return buffer.readUtf8()
}

private fun MoshiElement.write(writer: JsonWriter) {
    when(this) {
        is MoshiArray -> writeArray(writer)
        is MoshiObject -> writeObject(writer)
        is MoshiString -> writer.value(value)
        is MoshiNumber -> writer.value(value)
        is MoshiNull -> writer.nullValue()
        is MoshiBoolean -> writer.value(value)
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

fun MoshiElement.Companion.parse(json: String): MoshiElement {
    JsonReader.of(json.byteInputStream().source().buffer()).use { reader ->
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
