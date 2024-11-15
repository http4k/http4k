package org.http4k.connect.amazon.sqs.model

import dev.forkhandles.values.NonBlankStringValueFactory
import org.http4k.connect.amazon.core.model.ResourceId
import org.http4k.core.Uri

class QueueName private constructor(value: String) : ResourceId(value) {
    companion object : NonBlankStringValueFactory<QueueName>(::QueueName) {
        fun parse(uri: Uri) = QueueName.of(uri.path.substringAfterLast('/'))
    }
}
