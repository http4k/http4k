package org.http4k.format

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
    theValue?.forEach(::write) ?: nullValue()
}

fun JsonWriter.write(name: String, theValue: String?) {
    name(name)
    write(theValue)
}

fun JsonWriter.write(theValue: String?) {
    theValue?.also { value(it) } ?: nullValue()
}
