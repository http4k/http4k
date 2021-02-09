package org.http4k.cloudevents

import io.cloudevents.SpecVersion
import io.cloudevents.core.data.BytesCloudEventData
import io.cloudevents.core.message.impl.BaseGenericBinaryMessageReaderImpl
import io.cloudevents.core.message.impl.GenericStructuredMessageReader
import io.cloudevents.core.message.impl.MessageUtils.parseStructuredOrBinaryMessage
import io.cloudevents.rw.CloudEventRWException
import org.http4k.core.Request
import java.util.function.BiConsumer
import java.util.function.Consumer

internal fun Request.toCloudEventReader() = parseStructuredOrBinaryMessage(
    { header(CONTENT_TYPE) },
    { eventFormat ->
        body
            .takeIf { it.length ?: 0 > 0 }
            ?.let { GenericStructuredMessageReader(eventFormat, it.payload.array()) }
            ?: throw CloudEventRWException.newOther(
                IllegalStateException("Message was not " + eventFormat.serializedContentType())
            )
    },
    { header(SPEC_VERSION) },
    { specVersion -> BinaryMessageReader(specVersion, this) }
)

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

internal const val CE_PREFIX = "ce-"
internal const val SPEC_VERSION = CE_PREFIX + "specversion"
internal const val CONTENT_TYPE = CE_PREFIX + "datacontenttype"
