package org.http4k.format

object ServerlessMoshi : org.http4k.format.ConfigurableMoshi(
    com.squareup.moshi.Moshi.Builder()
        .add { type, _, moshi ->
            when {
                type === LinkedHashMap::class.java -> moshi.adapter(Map::class.java)
                else -> null
            }
        }
        .asConfigurable()
        .withStandardMappings()
        .done()
)
