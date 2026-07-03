package org.http4k.webdriver.datastar

import org.http4k.format.Moshi
import org.http4k.format.MoshiNode
import org.http4k.format.MoshiObject
import org.http4k.format.unwrap
import org.http4k.format.wrap

/**
 * A nested store of datastar signals. Values are JSON-shaped: String, Double, Boolean, null,
 * Map<String, Any?> or List<Any?>. Paths are dot-separated (e.g. "user.name").
 */
internal class SignalStore {
    private val root = linkedMapOf<String, Any?>()

    operator fun get(path: String): Any? =
        path.split('.').fold(root as Any?) { current, segment -> (current as? Map<*, *>)?.get(segment) }

    operator fun set(path: String, value: Any?) {
        val segments = path.split('.')
        var current: MutableMap<String, Any?> = root
        for (segment in segments.dropLast(1)) {
            val next = current[segment]
            @Suppress("UNCHECKED_CAST")
            current = if (next is MutableMap<*, *>) {
                next as MutableMap<String, Any?>
            } else {
                linkedMapOf<String, Any?>().also { current[segment] = it }
            }
        }
        current[segments.last()] = value
    }

    fun contains(path: String): Boolean {
        val segments = path.split('.')
        val parent = segments.dropLast(1).fold(root as Any?) { current, segment -> (current as? Map<*, *>)?.get(segment) }
        return (parent as? Map<*, *>)?.containsKey(segments.last()) == true
    }

    /**
     * Deep-merges the values into the store. A null value deletes the signal (unless onlyIfMissing).
     */
    fun patch(values: Map<String, Any?>, onlyIfMissing: Boolean = false) {
        merge(root, values, onlyIfMissing)
    }

    fun isEmpty() = root.isEmpty()

    fun clear() = root.clear()

    fun toJson(): String = renderJson(root)

    /** Signals with a path segment prefixed with _ are local-only and are not sent to the backend. */
    fun toTransportJson(): String = renderJson(withoutLocal(root))

    private fun withoutLocal(map: Map<*, *>): Map<*, *> = map.entries
        .filterNot { it.key.toString().startsWith("_") }
        .associate { (key, value) -> key to if (value is Map<*, *>) withoutLocal(value) else value }

    private fun merge(target: MutableMap<String, Any?>, patch: Map<*, *>, onlyIfMissing: Boolean) {
        patch.forEach { (key, value) ->
            val name = key.toString()
            when {
                value == null -> if (!onlyIfMissing) target.remove(name)

                value is Map<*, *> -> {
                    val existing = target[name]

                    @Suppress("UNCHECKED_CAST")
                    val nested = existing as? MutableMap<String, Any?>
                        ?: linkedMapOf<String, Any?>().also {
                            if (!(onlyIfMissing && target.containsKey(name))) target[name] = it
                        }
                    merge(nested, value, onlyIfMissing)
                }

                onlyIfMissing && target.containsKey(name) -> {}

                else -> target[name] = value
            }
        }
    }
}

@Suppress("UNCHECKED_CAST")
internal fun parseJsonObject(input: String): Map<String, Any?> =
    runCatching { Moshi.asA<MoshiObject>(input).unwrap() as Map<String, Any?> }.getOrDefault(emptyMap())

internal fun renderJson(value: Any?): String = Moshi.asFormatString(MoshiNode.wrap(value))

internal fun renderNumber(value: Double): String = renderJson(value)
