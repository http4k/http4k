package org.http4k.format

import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import org.http4k.events.Event
import java.lang.reflect.Type

/**
 * To implement custom JSON configuration, create your own object singleton. Extra mappings can be added before done() is called.
 */
object Moshi : ConfigurableMoshi(
    Moshi.Builder()
        .addLast(EventAdapter)
        .addLast(CollectionEdgeCasesAdapter)
        .asConfigurable()
        .withStandardMappings()
        .done()
)

object EventAdapter : JsonAdapter.Factory {
    override fun create(p0: Type, p1: MutableSet<out Annotation>, p2: Moshi) =
        if (p0.typeName == Event::class.java.typeName) p2.adapter(Any::class.java) else null
}

/**
 * This adapter takes care of edge cases when dealing with collections.
 */
object CollectionEdgeCasesAdapter : JsonAdapter.Factory {
    override fun create(type: Type, annotations: Set<Annotation>, moshi: Moshi) =
        with(Types.getRawType(type)) {
            when {
                isA(Map::class.java) -> moshi.adapter(Map::class.java)
                else -> null
            }
        }

    private fun Class<*>?.isA(testCase: Class<*>) =
        this?.let { testCase != this && testCase.isAssignableFrom(this) } ?: false
}
