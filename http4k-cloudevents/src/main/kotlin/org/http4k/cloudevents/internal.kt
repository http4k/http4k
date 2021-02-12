package org.http4k.cloudevents

import io.cloudevents.CloudEvent
import io.cloudevents.SpecVersion
import io.cloudevents.core.data.BytesCloudEventData
import io.cloudevents.core.message.MessageReader
import io.cloudevents.core.message.impl.BaseGenericBinaryMessageReaderImpl
import io.cloudevents.core.message.impl.GenericStructuredMessageReader
import io.cloudevents.core.provider.EventFormatProvider
import io.cloudevents.rw.CloudEventRWException.newUnknownEncodingException
import org.http4k.core.Body
import org.http4k.core.Body.Companion.EMPTY
import org.http4k.core.HttpMessage
import java.nio.ByteBuffer
import java.util.function.BiConsumer
import java.util.function.Consumer

internal fun HttpMessage.toCloudEventReader(): MessageReader {
    val byContentType = header("ce-datacontenttype")
        ?.let { EventFormatProvider.getInstance().resolveFormat(it) }
        ?.let { GenericStructuredMessageReader(it, body.payload.array()) }

    val bySpecVersion = header("ce-specversion")
        ?.let { SpecVersion.parse(it) }
        ?.let { BinaryMessageReader(it, this) }

    return byContentType ?: bySpecVersion ?: throw newUnknownEncodingException()
}

internal fun HttpMessage.write(cloudEvent: CloudEvent): HttpMessage =
    header("ce-datacontenttype", cloudEvent.dataContentType)
        .header("ce-specversion", cloudEvent.specVersion.toString()).body(
            cloudEvent.dataContentType
                ?.let { EventFormatProvider.getInstance().resolveFormat(it) }
                ?.serialize(cloudEvent)
                ?.let { Body(ByteBuffer.wrap(it)) }
                ?: EMPTY
        )

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

