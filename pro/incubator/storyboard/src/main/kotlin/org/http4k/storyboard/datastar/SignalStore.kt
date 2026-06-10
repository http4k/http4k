package org.http4k.storyboard.datastar

import com.squareup.moshi.Moshi

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
            current = if (next is MutableMap<*, *>) next as MutableMap<String, Any?>
            else linkedMapOf<String, Any?>().also { current[segment] = it }
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

private val json = Moshi.Builder().build().adapter(Any::class.java)

@Suppress("UNCHECKED_CAST")
internal fun parseJsonObject(input: String): Map<String, Any?> =
    runCatching { json.fromJson(input) }.getOrNull() as? Map<String, Any?> ?: emptyMap()

// hand-rolled rather than Moshi: integral doubles must render as JSON ints (2, not 2.0)
internal fun renderJson(value: Any?): String = when (value) {
    null -> "null"
    is Boolean -> value.toString()
    is Number -> renderNumber(value.toDouble())
    is String -> quote(value)
    is Map<*, *> -> value.entries.joinToString(",", "{", "}") { (k, v) -> "${quote(k.toString())}:${renderJson(v)}" }
    is List<*> -> value.joinToString(",", "[", "]") { renderJson(it) }
    else -> quote(value.toString())
}

internal fun renderNumber(value: Double): String =
    if (value.isFinite() && value == Math.floor(value) && Math.abs(value) < 1e15) value.toLong().toString()
    else value.toString()

private fun quote(value: String) = buildString {
    append('"')
    value.forEach {
        when (it) {
            '"' -> append("\\\"")
            '\\' -> append("\\\\")
            '\n' -> append("\\n")
            '\r' -> append("\\r")
            '\t' -> append("\\t")
            else -> if (it < ' ') append("\\u%04x".format(it.code)) else append(it)
        }
    }
    append('"')
}
