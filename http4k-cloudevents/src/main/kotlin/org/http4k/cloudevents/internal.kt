package org.http4k.cloudevents

import io.cloudevents.SpecVersion
import io.cloudevents.core.data.BytesCloudEventData
import io.cloudevents.core.message.MessageReader
import io.cloudevents.core.message.impl.BaseGenericBinaryMessageReaderImpl
import io.cloudevents.core.message.impl.GenericStructuredMessageReader
import io.cloudevents.core.provider.EventFormatProvider
import io.cloudevents.rw.CloudEventRWException.newUnknownEncodingException
import org.http4k.core.Request
import java.util.function.BiConsumer
import java.util.function.Consumer

internal fun Request.toCloudEventReader(eventFormatProvider: EventFormatProvider): MessageReader {
    val byContentType = header("ce-datacontenttype")
        ?.let { eventFormatProvider.resolveFormat(it) }
        ?.let { GenericStructuredMessageReader(it, body.payload.array()) }

    val bySpecVersion = header("ce-specversion")
        ?.let { SpecVersion.parse(it) }
        ?.let {
            BinaryMessageReader(it, this)
        }

    return byContentType ?: bySpecVersion ?: throw newUnknownEncodingException()
}

internal class BinaryMessageReader(version: SpecVersion, private val request: Request) :
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

