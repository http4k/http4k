package org.http4k.storage

import java.util.concurrent.ConcurrentHashMap

/**
 * Simple In-Memory, map-backed storage implementation.
 */
fun <T : Any> Storage.Companion.InMemory() = object : Storage<T> {
    private val byKey = ConcurrentHashMap<String, T>()

    override fun get(key: String): T? = byKey[key]
    override fun set(key: String, data: T) {
        byKey[key] = data
    }

    override fun remove(key: String) = byKey.remove(key) != null

    override fun removeAll(keyPrefix: String) =
        if (byKey.isEmpty()) false
        else {
            byKey.keys().iterator().forEach { if (it.startsWith(keyPrefix)) remove(it) }
            true
        }

    override fun keySet(keyPrefix: String) = byKey.keys.filter { it.startsWith(keyPrefix) }.toSet()

    override fun toString() = byKey.toString()
}
