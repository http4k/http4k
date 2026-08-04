package org.http4k.connect.amazon.iotdataplane.model

import dev.forkhandles.values.NonBlankStringValueFactory
import dev.forkhandles.values.StringValue

/**
 * Only checked for being non-blank. MQTT topics are slash-separated and can contain `$` (reserved
 * topics) and spaces, so the stricter pattern used for thing and shadow names would reject valid
 * topics.
 */
class TopicName private constructor(value: String) : StringValue(value) {
    companion object : NonBlankStringValueFactory<TopicName>(::TopicName)
}
