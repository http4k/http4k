package org.http4k.format

import com.squareup.moshi.JsonReader

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

fun <T> JsonReader.list(mk: () -> T, item: T.(String) -> Unit): List<T> {
    val items = mutableListOf<T>()
    beginArray()
    while (hasNext()) items += obj(mk, item)
    endArray()
    return items
}

fun <T> JsonReader.list(item: () -> T): List<T> {
    val items = mutableListOf<T>()
    beginArray()
    while (hasNext()) items += item()
    endArray()
    return items
}

fun JsonReader.stringList(): List<String> {
    beginArray()
    val list = mutableListOf<String>()
    while (hasNext()) list += nextString()
    endArray()
    return list
}

fun JsonReader.stringMap(): Map<String, String> {
    beginObject()
    val map = mutableMapOf<String, String>()
    while (hasNext()) map += nextName() to nextString()
    endObject()
    return map
}

fun <T> JsonReader.map(valueFn: () -> T): Map<String, T> {
    beginObject()
    val map = mutableMapOf<String, T>()
    while (hasNext()) map += nextName() to valueFn()
    endObject()
    return map
}

fun JsonReader.stringOrNull() = if (peek() == JsonReader.Token.NULL) nextNull() else nextString()
