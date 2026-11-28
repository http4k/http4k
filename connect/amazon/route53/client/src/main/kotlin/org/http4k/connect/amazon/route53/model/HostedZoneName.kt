package org.http4k.connect.amazon.route53.model

import dev.forkhandles.values.StringValue
import dev.forkhandles.values.ValueFactory
import dev.forkhandles.values.regex

class HostedZoneName private constructor(value: String) : StringValue(value) {
    companion object : ValueFactory<HostedZoneName, String>(
        coerceFn = ::HostedZoneName,
        parseFn = { if (it.endsWith('.')) it else "$it." },
        validation = "(?s).*\\.".regex
    )
}
