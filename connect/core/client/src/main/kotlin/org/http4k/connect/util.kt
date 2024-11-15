package org.http4k.connect

import kotlin.reflect.KClass

inline fun <reified T : Any> kClass(): KClass<T> = T::class

// TODO suggesting moving into http4k-core: KotlinExtensions.kt
fun <T: Any> StringBuilder.appendAll(items: Collection<T>, fn: (T) -> String): StringBuilder {
    for (item in items) {
        append(fn(item))
    }
    return this
}
