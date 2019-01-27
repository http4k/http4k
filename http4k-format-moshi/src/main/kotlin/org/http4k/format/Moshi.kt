package org.http4k.format

import com.squareup.moshi.Moshi
import com.squareup.moshi.Moshi.Builder
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import org.http4k.lens.BiDiMapping

/**
 * To implement custom JSON configuration, copy and modify this file
 */

object Moshi : ConfigurableMoshi(ConfigureMoshi().withStandardMappings())

class ConfigureMoshi : ConfigureAutoMarshallingJson<Builder> {
    private val builder = Moshi.Builder()

    override fun <T> text(mapping: BiDiMapping<String, T>) = builder.text(mapping)

    // add the Kotlin adapter last, as it will hjiack our custom mappings otherwise
    override fun done(): Builder = builder.add(KotlinJsonAdapterFactory())
}
