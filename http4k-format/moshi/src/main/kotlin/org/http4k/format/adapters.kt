package org.http4k.format

import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import java.lang.reflect.Type

/**
 * Convenience class to create Moshi Adapter Factory
 */
open class SimpleMoshiAdapterFactory(vararg typesToAdapters: Pair<String, (Moshi) -> JsonAdapter<*>>) : JsonAdapter.Factory {
    private val mappings = typesToAdapters.toMap()

    override fun create(type: Type, annotations: Set<Annotation>, moshi: Moshi) =
        mappings[Types.getRawType(type).typeName]?.let { it(moshi) }
}

/**
 * Convenience function to create Moshi Adapter.
 */
inline fun <reified T : JsonAdapter<K>, reified K> adapter(noinline fn: (Moshi) -> T) = K::class.java.name to fn

/**
 * Convenience function to create Moshi Adapter Factory for a simple Moshi Adapter
 */
inline fun <reified K> JsonAdapter<K>.asFactory() = SimpleMoshiAdapterFactory(K::class.java.name to { this })
