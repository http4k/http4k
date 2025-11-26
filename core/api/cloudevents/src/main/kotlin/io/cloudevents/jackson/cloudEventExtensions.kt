package io.cloudevents.jackson

import io.cloudevents.CloudEvent
import tools.jackson.databind.ObjectMapper
import java.io.ByteArrayInputStream
import java.io.StringWriter

class Http4kSerDe(private val mapper: ObjectMapper) {
    fun serialize(event: CloudEvent): ByteArray {
        val s = StringWriter()
        val generator = mapper.createGenerator(s)
        CloudEventSerializer().serialize(event, generator, null)
        generator.flush()
        return s.toString().toByteArray()
    }

    fun deserialize(bytes: ByteArray): CloudEvent {
        val parser = mapper.createParser(ByteArrayInputStream(bytes))
        parser.nextToken()
        return CloudEventDeserializer().deserialize(parser, null)
    }
}
