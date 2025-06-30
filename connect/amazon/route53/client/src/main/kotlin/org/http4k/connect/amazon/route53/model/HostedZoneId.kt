package org.http4k.connect.amazon.route53.model

import dev.forkhandles.values.StringValue
import dev.forkhandles.values.StringValueFactory
import dev.forkhandles.values.and
import dev.forkhandles.values.maxLength
import dev.forkhandles.values.minLength

class HostedZoneId private constructor(value: String) : StringValue(value) {
    companion object : StringValueFactory<HostedZoneId>(
        fn = ::HostedZoneId,
        validation = 1.minLength.and(32.maxLength)
    )
}
