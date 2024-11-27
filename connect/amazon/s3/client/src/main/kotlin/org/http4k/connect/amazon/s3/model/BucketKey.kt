package org.http4k.connect.amazon.s3.model

import dev.forkhandles.values.NonBlankStringValueFactory
import dev.forkhandles.values.StringValue

class BucketKey private constructor(value: String) : StringValue(value) {
    companion object : NonBlankStringValueFactory<BucketKey>(::BucketKey)
}
