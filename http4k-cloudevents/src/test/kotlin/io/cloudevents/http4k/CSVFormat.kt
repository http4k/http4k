package io.cloudevents.http4k

import io.cloudevents.CloudEvent
import io.cloudevents.SpecVersion
import io.cloudevents.core.builder.CloudEventBuilder
import io.cloudevents.core.data.BytesCloudEventData
import io.cloudevents.core.format.EventFormat
import io.cloudevents.rw.CloudEventDataMapper
import io.cloudevents.types.Time
import org.http4k.base64DecodedArray
import org.http4k.base64Encode
import java.net.URI
import java.nio.charset.StandardCharsets.UTF_8
import java.util.Objects
import java.util.regex.Pattern

class CSVFormat : EventFormat {
    override fun serialize(event: CloudEvent) = java.lang.String.join(
        ",",
        event.specVersion.toString(),
        event.id,
        event.type,
        event.source.toString(),
        Objects.toString(event.dataContentType),
        Objects.toString(event.dataSchema),
        Objects.toString(event.subject),
        if (event.time != null) Time.writeTime(event.time) else "null",
        if (event.data != null) event.data?.toBytes()?.base64Encode() ?: ByteArray(0).base64Encode()
        else "null"
    ).toByteArray()

    override fun deserialize(bytes: ByteArray, mapper: CloudEventDataMapper<*>): CloudEvent {
        val splitted = String(bytes, UTF_8).split(Pattern.quote(",").toRegex())
        val sv = SpecVersion.parse(splitted[0])
        val id = splitted[1]
        val type = splitted[2]
        val source = URI.create(splitted[3])
        val datacontenttype = if (splitted[4] == "null") null else splitted[4]
        val dataschema = if (splitted[5] == "null") null else URI.create(splitted[5])
        val subject = if (splitted[6] == "null") null else splitted[6]
        val time = if (splitted[7] == "null") null else Time.parseTime(splitted[7])
        val data = if (splitted[8] == "null") null else splitted[8].base64DecodedArray()
        val builder = CloudEventBuilder.fromSpecVersion(sv)
            .withId(id)
            .withType(type)
            .withSource(source)
        if (datacontenttype != null) builder.withDataContentType(datacontenttype)
        if (dataschema != null) builder.withDataSchema(dataschema)
        if (subject != null) builder.withSubject(subject)
        if (time != null) builder.withTime(time)
        if (data != null) builder.withData(mapper.map(BytesCloudEventData.wrap(data)))
        return builder.build()
    }

    override fun deserializableContentTypes(): Set<String> = setOf(serializedContentType())

    override fun serializedContentType(): String = "application/cloudevents+csv"
}
