package io.cloudevents.http4k

import io.cloudevents.CloudEvent
import io.cloudevents.CloudEventData
import io.cloudevents.core.builder.CloudEventBuilder
import io.cloudevents.core.format.EventDeserializationException
import io.cloudevents.core.format.EventFormat
import io.cloudevents.rw.CloudEventDataMapper
import io.cloudevents.rw.CloudEventRWException
import org.http4k.core.CLOUD_EVENT_JSON
import org.http4k.core.ContentType
import org.http4k.format.ConfigurableJackson
import java.io.IOException

/**
 * A custom EventFormat which uses all the standard mappings from a JSON type
 */
fun ConfigurableJackson.cloudEventsFormat(): EventFormat {
    return object : EventFormat {
        override fun serialize(event: CloudEvent) = mapper.writeValueAsBytes(event)

        override fun deserialize(bytes: ByteArray) = try {
            mapper.readValue(bytes, CloudEvent::class.java)
        } catch (e: IOException) {
            throw EventDeserializationException(e)
        }

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

        override fun serializedContentType() = ContentType.Companion.CLOUD_EVENT_JSON.value
    }
}
