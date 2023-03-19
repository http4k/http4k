package org.http4k.format

import com.beust.klaxon.Klaxon as KKlaxon

/**
 * To implement custom JSON configuration, create your own object singleton. Extra mappings can be added before done() is called.
 */
object Klaxon : ConfigurableKlaxon(KKlaxon()
    .asConfigurable()
    .withStandardMappings()
    .done()
)
