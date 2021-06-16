package org.http4k.format

import com.squareup.moshi.Moshi

/**
 * To implement custom JSON configuration, create your own object singleton. Extra mappings can be added before done() is called.
 */
object Moshi : ConfigurableMoshi(
    Moshi.Builder()
        .addLast(EventAdapter)
        .addLast(ThrowableAdapter)
        .addLast(CollectionEdgeCasesAdapter)
        .asConfigurable()
        .withStandardMappings()
        .done()
)
