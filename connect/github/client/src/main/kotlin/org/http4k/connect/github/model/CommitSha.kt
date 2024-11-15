package org.http4k.connect.github.model

import dev.forkhandles.values.NonBlankStringValueFactory
import dev.forkhandles.values.StringValue

class CommitSha private constructor(value: String) : StringValue(value) {
    companion object : NonBlankStringValueFactory<CommitSha>(::CommitSha)
}
