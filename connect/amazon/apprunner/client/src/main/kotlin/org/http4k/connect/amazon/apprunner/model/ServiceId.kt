package org.http4k.connect.amazon.apprunner.model

import dev.forkhandles.values.NonBlankStringValueFactory
import org.http4k.connect.amazon.core.model.ResourceId

class ServiceId private constructor(value: String) : ResourceId(value) {
    companion object : NonBlankStringValueFactory<ServiceId>(::ServiceId)
}

