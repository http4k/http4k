package org.http4k.format

import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Moshi
import org.http4k.events.Event
import java.lang.reflect.Type

/**
 * To implement custom JSON configuration, create your own object singleton. Extra mappings can be added before done() is called.
 */
object Moshi : ConfigurableMoshi(
    Moshi.Builder()
        .addLast(EventAdapter)
        .asConfigurable()
        .withStandardMappings()
        .done()
)

object EventAdapter : JsonAdapter.Factory {
    override fun create(p0: Type, p1: MutableSet<out Annotation>, p2: Moshi) =
        if (p0.typeName == Event::class.java.typeName) p2.adapter(Any::class.java) else null
}
