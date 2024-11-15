package org.http4k.connect.storage

import dev.forkhandles.values.Value

operator fun <V : Any> Storage<V>.get(key: Value<*>): V? = this[key.toString()]

fun <V : Any> Storage<V>.remove(key: Value<*>) = remove(key.toString())

operator fun <T : Any> Storage<T>.minusAssign(key: Value<*>) = minusAssign(key.value.toString())

operator fun <V : Any> Storage<V>.set(key: Value<*>, v: V) {
    this[key.toString()] = v
}

fun <T : Any> Storage<T>.getOrPut(key: Value<*>, fn: () -> T) = this[key.toString()] ?: run {
    fn().also { this[key.toString()] = it }
}

