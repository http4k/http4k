package org.http4k.connect.amazon.instancemetadata.model

import dev.forkhandles.values.StringValue
import dev.forkhandles.values.StringValueFactory
import dev.forkhandles.values.toValidator

class ImageId private constructor(value: String) : StringValue(value) {
    companion object : StringValueFactory<ImageId>(::ImageId, { it: String -> it.startsWith("ami-") }.toValidator())
}
