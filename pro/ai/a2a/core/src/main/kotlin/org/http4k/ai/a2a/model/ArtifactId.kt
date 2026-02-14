package org.http4k.ai.a2a.model

import dev.forkhandles.values.NonBlankStringValueFactory
import dev.forkhandles.values.StringValue

class ArtifactId private constructor(value: String) : StringValue(value) {
    companion object : NonBlankStringValueFactory<ArtifactId>(::ArtifactId)
}
