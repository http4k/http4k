package org.http4k.format

import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import org.http4k.events.Event
import java.lang.reflect.Type
import kotlin.reflect.KClass

/**
 * Convenience class to create Moshi Adapter Factory
 */
open class SimpleMoshiAdapterFactory(vararg typesToAdapters: Pair<String, (Moshi) -> JsonAdapter<*>>) :
    JsonAdapter.Factory {
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

/**
 * Convenience function to add a custom adapter.
 */
inline fun <reified T : JsonAdapter<K>, reified K> Moshi.Builder.addTyped(fn: T): Moshi.Builder =
    add(K::class.java, fn)


/**
 * This adapter factory will capture ALL instances of a particular superclass/interface.
 */
abstract class IsAnInstanceOfAdapter<T : Any>(private val clazz: KClass<T>) : JsonAdapter.Factory {
    override fun create(type: Type, annotations: Set<Annotation>, moshi: Moshi) =
        with(Types.getRawType(type)) {
            when {
                isA(clazz.java) -> moshi.adapter(clazz.java)
                else -> null
            }
        }

    private fun Class<*>?.isA(testCase: Class<*>): Boolean =
        this?.let { testCase != this && testCase.isAssignableFrom(this) } ?: false
}

/**
 * These adapters are the edge case adapters for dealing with Moshi
 */

object ThrowableAdapter : IsAnInstanceOfAdapter<Throwable>(Throwable::class)

object MapAdapter : IsAnInstanceOfAdapter<Map<*, *>>(Map::class)

object ListAdapter : IsAnInstanceOfAdapter<List<*>>(List::class)

object SetAdapter : IsAnInstanceOfAdapter<Set<*>>(Set::class)

object EventAdapter : JsonAdapter.Factory {
    override fun create(p0: Type, p1: MutableSet<out Annotation>, p2: Moshi) =
        if (p0.typeName == Event::class.java.typeName) p2.adapter(Any::class.java) else null
}
