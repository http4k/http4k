package org.http4k.format

import com.squareup.moshi.JsonReader
import com.squareup.moshi.JsonReader.Token.NULL

fun <T> JsonReader.obj(mk: () -> T, fn: T.(String) -> Unit): T {
    beginObject()
    val item = mk()
    while (hasNext()) item.fn(nextName())
    endObject()
    return item
}

fun <T> JsonReader.obj(build: (Map<String, Any>) -> T, item: (String) -> Any): T {
    beginObject()
    val map = mutableMapOf<String, Any>()
    while (hasNext()) nextName().also { map[it] = item(it) }
    return build(map).also { endObject() }
}

fun <T> JsonReader.list(mk: () -> T, item: T.(String) -> Unit) = skipNullOr {
    val items = mutableListOf<T?>()
    beginArray()
    while (hasNext()) items += obj(mk, item)
    endArray()
    items
}

fun <T> JsonReader.list(item: () -> T) = skipNullOr {
    val items = mutableListOf<T>()
    beginArray()
    while (hasNext()) items += item()
    endArray()
    items
}

fun JsonReader.stringList() = skipNullOr {
    beginArray()
    val list = mutableListOf<String>()
    while (hasNext()) list += nextString()
    endArray()
    list
}

fun JsonReader.stringMap(): Map<String, String>? = skipNullOr {
    beginObject()
    val map = mutableMapOf<String, String>()
    while (hasNext()) map += nextName() to nextString()
    endObject()
    map
}

fun <T> JsonReader.map(valueFn: () -> T) = skipNullOr {
    beginObject()
    val map = mutableMapOf<String, T>()
    while (hasNext()) map += nextName() to valueFn()
    endObject()
    map
}

fun JsonReader.stringOrNull() = if (peek() == NULL) nextNull() else nextString()

private fun <T> JsonReader.skipNullOr(fn: JsonReader.() -> T) = if (peek() == NULL) skipValue().let { null } else fn()
