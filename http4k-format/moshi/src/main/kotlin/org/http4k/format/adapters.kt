package org.http4k.format

import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.JsonReader
import com.squareup.moshi.JsonWriter
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import dev.forkhandles.values.AbstractValue
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
abstract class IsAnInstanceOfAdapter<T : Any>(
    private val clazz: KClass<T>,
    private val resolveAdapter: Moshi.(KClass<T>) -> JsonAdapter<T> = { adapter(it.java) }
) : JsonAdapter.Factory {
    override fun create(type: Type, annotations: Set<Annotation>, moshi: Moshi) =
        with(Types.getRawType(type)) {
            when {
                isA(clazz.java) -> moshi.resolveAdapter(clazz)
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

private fun <T : Any> writer(fn: JsonWriter.(T) -> Unit) = object : JsonAdapter<T>() {
    override fun fromJson(p0: JsonReader) = throw UnsupportedOperationException("This adapter is write-only")
    override fun toJson(p0: JsonWriter, p1: T?) {
        p1?.let { p0.fn(it) } ?: p0.nullValue()
    }
}

object MoshiNodeAdapter : JsonAdapter.Factory {
    override fun create(p0: Type, p1: MutableSet<out Annotation>, p2: Moshi): JsonAdapter<out MoshiNode>? =
        when (p0.typeName) {
            MoshiArray::class.java.typeName -> null
            MoshiObject::class.java.typeName -> null
            MoshiString::class.java.typeName -> writer<MoshiString> { value(it.value) }
            MoshiInteger::class.java.typeName -> writer<MoshiInteger> { value(it.value)  }
            MoshiDecimal::class.java.typeName ->  writer<MoshiDecimal> { value(it.value)  }
            MoshiBoolean::class.java.typeName ->  writer<MoshiBoolean> { value(it.value)  }
            MoshiNull::class.java.typeName ->  writer<MoshiNull> { nullValue()  }
            else -> null
        }
}

object ProhibitUnknownValuesAdapter : JsonAdapter.Factory {
    override fun create(type: Type, annotations: Set<Annotation>, moshi: Moshi) =
        when {
            (type as Class<*>).superclass == AbstractValue::class.java -> throw UnmappedValue(type)
            else -> null
        }
}

class UnmappedValue(type: Type) : Exception("unmapped type $type")
