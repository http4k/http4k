package org.http4k.connect.amazon.secretsmanager.model

import dev.forkhandles.values.NonBlankStringValueFactory
import dev.forkhandles.values.StringValue

class VersionId private constructor(value: String) : StringValue(value) {
    companion object : NonBlankStringValueFactory<VersionId>(::VersionId)
}
