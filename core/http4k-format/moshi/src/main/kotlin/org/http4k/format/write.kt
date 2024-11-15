package org.http4k.format

import com.squareup.moshi.JsonWriter

fun JsonWriter.string(name: String, theValue: String?) {
    name(name)
    theValue?.also(::value) ?: nullValue()
}

fun JsonWriter.number(name: String, theValue: Number?) {
    name(name)
    theValue?.also(::value) ?: nullValue()
}

fun JsonWriter.boolean(name: String, theValue: Boolean?) {
    name(name)
    theValue?.also(::value) ?: nullValue()
}

fun JsonWriter.obj(name: String, theValue: Map<String, Any?>?) {
    name(name)
    obj(theValue) {
        entries.forEach { string(it.key, it.value?.toString()) }
    }
}

fun <T> JsonWriter.obj(name: String, obj: T?, fn: T.() -> Unit) {
    name(name)
    obj(obj, fn)
}

fun <T> JsonWriter.obj(obj: T?, fn: T.() -> Unit) {
    when (obj) {
        null -> nullValue()
        else -> {
            beginObject()
            obj.fn()
            endObject()
        }
    }
}

fun JsonWriter.list(name: String, theValue: List<String>?) {
    name(name)
    list(theValue, ::value)
}

fun <T> JsonWriter.list(name: String, list: List<T>?, fn: T.() -> Unit) {
    name(name)
    list(list, fn)
}

fun <T> JsonWriter.list(list: List<T>?, fn: T.() -> Unit) {
    when (list) {
        null -> nullValue()
        else -> {
            beginArray()
            list.forEach(fn)
            endArray()
        }
    }
}
