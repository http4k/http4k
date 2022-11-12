package org.http4k.format

import com.beust.klaxon.Klaxon as KKlaxon

private fun standardConfig() = KKlaxon()
    .asConfigurable()
    .withStandardMappings()

/**
 * To implement custom JSON configuration, create your own object singleton. Extra mappings can be added before done() is called.
 */
object Klaxon : ConfigurableKlaxon(standardConfig().done()) {
    fun update(
        configureFn: AutoMappingConfiguration<KKlaxon>.() -> AutoMappingConfiguration<KKlaxon>
    ) = ConfigurableKlaxon(standardConfig().let(configureFn).done())
}
