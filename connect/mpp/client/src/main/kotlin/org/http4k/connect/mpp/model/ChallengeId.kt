package org.http4k.connect.mpp.model

import dev.forkhandles.values.NonBlankStringValueFactory
import dev.forkhandles.values.StringValue

class ChallengeId private constructor(value: String) : StringValue(value) {
    companion object : NonBlankStringValueFactory<ChallengeId>(::ChallengeId)
}
