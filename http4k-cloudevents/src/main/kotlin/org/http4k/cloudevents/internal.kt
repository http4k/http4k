package org.http4k.cloudevents

import io.cloudevents.CloudEvent
import io.cloudevents.SpecVersion
import io.cloudevents.core.data.BytesCloudEventData
import io.cloudevents.core.message.MessageReader
import io.cloudevents.core.message.impl.BaseGenericBinaryMessageReaderImpl
import io.cloudevents.core.message.impl.GenericStructuredMessageReader
import io.cloudevents.core.provider.EventFormatProvider
import io.cloudevents.rw.CloudEventRWException.newUnknownEncodingException
import io.cloudevents.types.Time.writeTime
import org.http4k.core.Body
import org.http4k.core.Body.Companion.EMPTY
import org.http4k.core.HttpMessage
import org.http4k.lens.Header.CONTENT_TYPE
import java.nio.ByteBuffer
import java.util.function.BiConsumer
import java.util.function.Consumer

internal fun HttpMessage.toCloudEventReader(): MessageReader {
    val byContentType = header("content-type")
        ?.let { EventFormatProvider.getInstance().resolveFormat(it) }
        ?.let { GenericStructuredMessageReader(it, body.payload.array()) }

    val bySpecVersion = header("ce-specversion")
        ?.let { SpecVersion.parse(it) }
        ?.let { BinaryMessageReader(it, this) }

    return byContentType ?: bySpecVersion ?: throw newUnknownEncodingException()
}

internal fun HttpMessage.write(cloudEvent: CloudEvent): HttpMessage = this
    .addHeaderIfPresent("id", cloudEvent.id)
    .addHeaderIfPresent("datacontenttype", cloudEvent.dataContentType)
    .addHeaderIfPresent("schemaurl", cloudEvent.dataSchema?.toString())
    .addHeaderIfPresent("source", cloudEvent.source?.toString())
    .addHeaderIfPresent("specversion", cloudEvent.specVersion.toString())
    .addHeaderIfPresent("subject", cloudEvent.subject)
    .addHeaderIfPresent("time", cloudEvent.time?.let(::writeTime))
    .addHeaderIfPresent("type", cloudEvent.type)
    .body(
        CONTENT_TYPE(this)
            ?.let { EventFormatProvider.getInstance().resolveFormat(it.toHeaderValue()) }
            ?.serialize(cloudEvent)
            ?.let { Body(ByteBuffer.wrap(it)) }
            ?: EMPTY
    )

private fun HttpMessage.addHeaderIfPresent(suffix: String, value: String?) =
    value?.let { header("ce-$suffix", it) } ?: this

internal class BinaryMessageReader(version: SpecVersion, private val request: HttpMessage) :
    BaseGenericBinaryMessageReaderImpl<String, String>(
        version,
        request.body.takeIf { it.length ?: 0 > 0 }
            ?.let { BytesCloudEventData.wrap(request.body.payload.array()) }
    ) {

    override fun isContentTypeHeader(key: String) = key.equals("Content-Type", ignoreCase = true)

    override fun isCloudEventsHeader(key: String) = key.toLowerCase().startsWith("ce-")

    override fun toCloudEventsKey(key: String) = key.substring(3).toLowerCase()

    override fun forEachHeader(fn: BiConsumer<String, String?>) {
        request.headers.forEach(Consumer { e -> fn.accept(e.first, e.second) })
    }

    override fun toCloudEventsValue(value: String): String = value
}

