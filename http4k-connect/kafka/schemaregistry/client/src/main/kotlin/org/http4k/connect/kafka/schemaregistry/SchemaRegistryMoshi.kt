package org.http4k.connect.kafka.schemaregistry

import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Moshi
import org.apache.avro.Schema
import org.http4k.connect.kClass
import org.http4k.connect.kafka.schemaregistry.model.SchemaId
import org.http4k.connect.kafka.schemaregistry.model.SchemaName
import org.http4k.connect.kafka.schemaregistry.model.Subject
import org.http4k.connect.kafka.schemaregistry.model.Version
import org.http4k.format.ConfigurableMoshi
import org.http4k.format.IsAnInstanceOfAdapter
import org.http4k.format.ListAdapter
import org.http4k.format.MapAdapter
import org.http4k.format.asConfigurable
import org.http4k.format.value
import org.http4k.format.withStandardMappings
import org.http4k.lens.BiDiMapping
import se.ansman.kotshi.KotshiJsonAdapterFactory

object SchemaRegistryMoshi : ConfigurableMoshi(
    Moshi.Builder()
        .add(SchemaRegistryJsonAdapterFactory)
        .add(ListAdapter)
        .add(MapAdapter)
        .add(object : IsAnInstanceOfAdapter<Schema>(kClass()) {})
        .asConfigurable()
        .withStandardMappings()
        .value(SchemaId)
        .value(SchemaName)
        .text(BiDiMapping(Schema::class.java, { Schema.Parser().parse(it) }, Schema::toString))
        .value(Subject)
        .value(Version)
        .done()
)

@KotshiJsonAdapterFactory
object SchemaRegistryJsonAdapterFactory : JsonAdapter.Factory by KotshiSchemaRegistryJsonAdapterFactory
