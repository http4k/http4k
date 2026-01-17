package io.cloudevents.http4k

import io.cloudevents.CloudEvent
import io.cloudevents.CloudEventData
import io.cloudevents.core.builder.CloudEventBuilder
import io.cloudevents.core.format.EventDeserializationException
import io.cloudevents.core.format.EventFormat
import io.cloudevents.jackson.Http4kSerDe
import io.cloudevents.rw.CloudEventDataMapper
import io.cloudevents.rw.CloudEventRWException
import org.http4k.core.CLOUD_EVENT_JSON
import org.http4k.core.ContentType
import org.http4k.format.ConfigurableJackson

/**
 * A custom EventFormat which uses all the standard mappings from a JSON type
 */
fun ConfigurableJackson.cloudEventsFormat() = object : EventFormat {
    private val serDe = Http4kSerDe(mapper)

    override fun serialize(event: CloudEvent) = serDe.serialize(event)

    override fun deserialize(bytes: ByteArray) =
        serDe.deserialize(bytes)

    override fun deserialize(bytes: ByteArray, mapper: CloudEventDataMapper<out CloudEventData>): CloudEvent {
        val deserialized = deserialize(bytes)
        return when (val data = deserialized.data) {
            null -> deserialized
            else -> try {
                CloudEventBuilder.from(deserialized).withData(mapper.map(data)).build()
            } catch (e: CloudEventRWException) {
                throw EventDeserializationException(e)
            }
        }
    }

    override fun serializedContentType() = ContentType.CLOUD_EVENT_JSON.value
}
