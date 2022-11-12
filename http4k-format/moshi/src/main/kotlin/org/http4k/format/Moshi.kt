package org.http4k.format

import com.squareup.moshi.Moshi

private fun standardConfig() = Moshi.Builder()
    .addLast(EventAdapter)
    .addLast(ThrowableAdapter)
    .addLast(ListAdapter)
    .addLast(MapAdapter)
    .asConfigurable()
    .withStandardMappings()

/**
 * To implement custom JSON configuration, create your own object singleton. Extra mappings can be added before done() is called.
 */
object Moshi : ConfigurableMoshi(
    standardConfig().done()
) {
    fun update(
        configureFn: AutoMappingConfiguration<Moshi.Builder>.() -> AutoMappingConfiguration<Moshi.Builder>
    ) = ConfigurableMoshi(standardConfig().let(configureFn).done())
}
