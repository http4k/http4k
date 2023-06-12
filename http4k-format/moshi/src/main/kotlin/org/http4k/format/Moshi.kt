package org.http4k.format

import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory

private fun standardConfig(kotlinFactory: JsonAdapter.Factory = KotlinJsonAdapterFactory()) = Moshi.Builder()
    .addLast(EventAdapter)
    .addLast(ThrowableAdapter)
    .addLast(ListAdapter)
    .addLast(SetAdapter)
    .addLast(MapAdapter)
    .asConfigurable(kotlinFactory)
    .withStandardMappings()

/**
 * To implement custom JSON configuration, create your own object singleton. Extra mappings can be added before done() is called.
 */
object Moshi : ConfigurableMoshi(standardConfig().done()) {
    fun custom(configureFn: AutoMappingConfiguration<Moshi.Builder>.() -> AutoMappingConfiguration<Moshi.Builder>) =
        ConfigurableMoshi(standardConfig().let(configureFn).done())
}

