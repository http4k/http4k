package org.http4k.connect.kafka.rest

import com.squareup.moshi.FromJson
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.JsonReader
import com.squareup.moshi.JsonWriter
import com.squareup.moshi.Moshi
import com.squareup.moshi.ToJson
import com.squareup.moshi.Types
import org.apache.avro.Schema
import org.apache.avro.generic.GenericContainer
import org.apache.avro.io.EncoderFactory
import org.apache.avro.specific.SpecificDatumWriter
import org.http4k.connect.kafka.rest.model.BrokerId
import org.http4k.connect.kafka.rest.model.ConsumerGroup
import org.http4k.connect.kafka.rest.model.ConsumerInstance
import org.http4k.connect.kafka.rest.model.ConsumerRequestTimeout
import org.http4k.connect.kafka.rest.model.Offset
import org.http4k.connect.kafka.rest.model.PartitionId
import org.http4k.connect.kafka.rest.model.SchemaId
import org.http4k.connect.kafka.rest.model.Topic
import org.http4k.connect.kafka.rest.v2.model.Record
import org.http4k.connect.kafka.rest.v2.model.Records
import org.http4k.connect.kafka.rest.v3.model.ClusterId
import org.http4k.connect.kafka.rest.v3.model.RecordData
import org.http4k.connect.kafka.rest.v3.model.RecordData.Binary
import org.http4k.connect.kafka.rest.v3.model.RecordData.Json
import org.http4k.connect.kafka.rest.v3.model.ResourceName
import org.http4k.connect.model.Base64Blob
import org.http4k.core.ContentType.Companion.APPLICATION_JSON
import org.http4k.format.ConfigurableMoshi
import org.http4k.format.IsAnInstanceOfAdapter
import org.http4k.format.ListAdapter
import org.http4k.format.MapAdapter
import org.http4k.format.SimpleMoshiAdapterFactory
import org.http4k.format.asConfigurable
import org.http4k.format.value
import org.http4k.format.withStandardMappings
import org.http4k.lens.BiDiMapping
import se.ansman.kotshi.KotshiJsonAdapterFactory
import java.io.ByteArrayOutputStream
import java.time.Duration

object KafkaRestMoshi : ConfigurableMoshi(
    Moshi.Builder()
        .add(KafkaRestJsonAdapterFactory)
        .add(ListAdapter)
        .add(SimpleMoshiAdapterFactory(Records::class.qualifiedName!! to { RecordsJsonAdapter(it) }))
        .add(SimpleMoshiAdapterFactory(RecordData::class.qualifiedName!! to { RecordDataJsonAdapter(it) }))
        .add(GenericContainerAdapter)
        .add(object : IsAnInstanceOfAdapter<GenericContainer>(GenericContainer::class) {})
        .add(MapAdapter)
        .asConfigurable()
        .withStandardMappings()
        .value(Base64Blob)
        .value(BrokerId)
        .value(ResourceName)
        .value(ClusterId)
        .value(ConsumerGroup)
        .value(ConsumerInstance)
        .text(BiDiMapping(ConsumerRequestTimeout::class.java,
            { ConsumerRequestTimeout.of(Duration.ofMillis(it.toLong())) }, { it.value.toMillis().toString() })
        )
        .text(BiDiMapping(Schema::class.java, { Schema.Parser().parse(it) }, Schema::toString))
        .value(ConsumerInstance)
        .value(Offset)
        .value(PartitionId)
        .value(SchemaId)
        .value(Topic)
        .done(),
    defaultContentType = APPLICATION_JSON.withNoDirectives()
)

@KotshiJsonAdapterFactory
object KafkaRestJsonAdapterFactory : JsonAdapter.Factory by KotshiKafkaRestJsonAdapterFactory

object GenericContainerAdapter : JsonAdapter<GenericContainer>() {
    @FromJson
    override fun fromJson(reader: JsonReader): GenericContainer? = throw UnsupportedOperationException()

    @ToJson
    override fun toJson(writer: JsonWriter, value: GenericContainer?) {
        value?.let { writer.jsonValue(KafkaRestMoshi.asA<Map<String, Any>>(it.toAvroData())) } ?: writer.nullValue()
    }

    private fun <T : GenericContainer> T.toAvroData() = ByteArrayOutputStream().let {
        EncoderFactory.get().jsonEncoder(schema, it, false).also {
            SpecificDatumWriter<T>(schema).write(this, it)
            it.flush()
        }
        String(it.toByteArray())
    }
}

class RecordsJsonAdapter(moshi: Moshi) : JsonAdapter<Records>() {
    private val recordsAdapter: JsonAdapter<List<Record<*, Any>>> = moshi.adapter(
        Types.newParameterizedType(
            List::class.javaObjectType,
            Types.newParameterizedType(
                Record::class.javaObjectType, Any::class.javaObjectType,
                Any::class.javaObjectType
            )
        ),
        setOf(),
        "records"
    )

    override fun toJson(writer: JsonWriter, `value`: Records?) {
        if (`value` == null) {
            writer.nullValue()
            return
        }
        writer
            .beginObject()
            .name("records").apply { recordsAdapter.toJson(this, `value`.records) }
            .name("key_schema").value(`value`.key_schema?.toString())
            .name("value_schema").value(`value`.value_schema?.toString())
            .endObject()
    }

    override fun fromJson(reader: JsonReader) = throw UnsupportedOperationException()
}

class RecordDataJsonAdapter(private val moshi: Moshi) : JsonAdapter<RecordData<*>>() {

    override fun toJson(writer: JsonWriter, `value`: RecordData<*>?) {
        if (`value` == null) {
            writer.nullValue()
            return
        }
        writer
            .beginObject()
            .name("type").value(`value`.type.name)
            .name("data").apply {
                when (`value`) {
                    is Binary -> value(`value`.data.value)
                    is Json -> jsonValue(`value`.data)
                }
            }
            .endObject()
    }

    override fun fromJson(reader: JsonReader): RecordData<*> = throw UnsupportedOperationException()
}
