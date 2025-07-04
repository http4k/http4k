package org.http4k.connect.amazon.route53.model

import dev.forkhandles.values.StringValue
import dev.forkhandles.values.StringValueFactory
import dev.forkhandles.values.ValueFactory
import dev.forkhandles.values.and
import dev.forkhandles.values.maxLength
import dev.forkhandles.values.minLength
import dev.forkhandles.values.regex

private const val validator = "^[A-Z0-9]+$"
class HostedZoneId private constructor(value: String) : StringValue(value) {
    companion object : ValueFactory<HostedZoneId, String>(
        coerceFn = ::HostedZoneId,
        parseFn = { it.split("/").last() }, // often comes in the form '/hostedzone/<id>'
        validation = 1.minLength.and(32.maxLength).and(validator.regex)
    )
}
