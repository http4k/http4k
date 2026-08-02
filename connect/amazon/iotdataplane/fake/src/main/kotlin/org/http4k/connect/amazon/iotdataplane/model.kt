package org.http4k.connect.amazon.iotdataplane

import org.http4k.connect.amazon.iotdataplane.model.TopicName
import org.http4k.connect.model.Base64Blob

/**
 * A message recorded by FakeIotDataPlane. The header fields are kept in their raw wire form so
 * tests can assert on exactly what was transmitted.
 */
data class PublishedMessage(
    val topic: TopicName,
    val payload: Base64Blob,
    val qos: Int? = null,
    val retain: Boolean? = null,
    val contentType: String? = null,
    val messageExpiry: Long? = null,
    val responseTopic: String? = null,
    val correlationData: String? = null,
    val payloadFormatIndicator: String? = null,
    val userProperties: String? = null
)
