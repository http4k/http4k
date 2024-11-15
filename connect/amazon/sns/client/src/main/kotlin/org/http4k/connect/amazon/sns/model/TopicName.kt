package org.http4k.connect.amazon.sns.model

import dev.forkhandles.values.NonBlankStringValueFactory
import org.http4k.connect.amazon.core.model.ResourceId

class TopicName private constructor(value: String) : ResourceId(value) {
    companion object : NonBlankStringValueFactory<TopicName>(::TopicName)
}
