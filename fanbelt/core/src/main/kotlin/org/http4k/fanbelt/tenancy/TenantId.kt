package org.http4k.fanbelt.tenancy

import dev.forkhandles.values.StringValue
import dev.forkhandles.values.StringValueFactory
import dev.forkhandles.values.regex
import org.http4k.fanbelt.bus.Topic

class TenantId private constructor(value: String) : StringValue(value) {
    operator fun invoke(topic: Topic) = topic.placedIn(value)

    companion object : StringValueFactory<TenantId>(::TenantId, "[A-Za-z0-9_-]{1,100}".regex)
}
