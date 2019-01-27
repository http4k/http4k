package org.http4k.format

import com.google.gson.GsonBuilder
import org.http4k.lens.BiDiMapping

/**
 * To implement custom JSON configuration, copy and modify this file
 */

object Gson : ConfigurableGson(ConfigureGsonBuilder().withStandardMappings())

class ConfigureGsonBuilder : ConfigureAutoMarshallingJson<GsonBuilder> {
    private val builder = GsonBuilder().serializeNulls()

    override fun <T> text(mapping: BiDiMapping<String, T>) = builder.text(mapping)

    override fun done(): GsonBuilder = builder
}
