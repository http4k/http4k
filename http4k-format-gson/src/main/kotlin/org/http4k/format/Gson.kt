package org.http4k.format

import com.google.gson.GsonBuilder

/**
 * To implement custom JSON configuration, create your own object singleton. Extra mappings can be added before done() is called.
 */
object Gson : ConfigurableGson(
    GsonBuilder()
        .serializeNulls()
        .asConfigurable()
        .withStandardMappings()
        .done()
)
