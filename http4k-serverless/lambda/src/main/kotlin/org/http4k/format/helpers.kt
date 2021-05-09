package org.http4k.format

import com.squareup.moshi.JsonReader
import com.squareup.moshi.JsonWriter

fun JsonWriter.write(name: String, theValue: Map<String, Any?>?) {
    name(name)
    theValue?.also {
        beginObject()
        it.entries.forEach { write(it.key, it.value?.toString()) }
        endObject()
    } ?: nullValue()
}

fun JsonWriter.write(name: String, theValue: List<String>?) {
    name(name)
    theValue?.let {
        beginArray()
        it.forEach(::write)
        endArray()
    } ?: nullValue()
}

fun JsonWriter.write(name: String, theValue: String?) {
    name(name)
    write(theValue)
}

fun JsonWriter.write(theValue: String?) {
    theValue?.also { value(it) } ?: nullValue()
}

fun JsonReader.readStringList(): List<String> {
    beginArray()
    val list = mutableListOf<String>()
    while (hasNext()) {
        list += nextString()
    }
    endArray()
    return list
}

fun JsonReader.readMap(): Map<String, String> {
    beginObject()
    val map = mutableMapOf<String, String>()
    while (hasNext()) {
        map += nextName() to nextString()
    }
    endObject()
    return map
}
