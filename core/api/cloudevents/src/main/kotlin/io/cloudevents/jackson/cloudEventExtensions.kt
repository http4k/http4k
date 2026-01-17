package io.cloudevents.jackson

import io.cloudevents.CloudEvent
import tools.jackson.databind.ObjectMapper
import java.io.ByteArrayInputStream
import java.io.StringWriter

class Http4kSerDe(private val mapper: ObjectMapper) {
    fun serialize(event: CloudEvent): ByteArray {
        val s = StringWriter()
        CloudEventSerializer().serialize(event, mapper.createGenerator(s), null)
        return String(s.buffer).toByteArray()
    }

    fun deserialize(bytes: ByteArray) = CloudEventDeserializer()
        .deserialize(mapper.createParser(ByteArrayInputStream(bytes)), null)
}
