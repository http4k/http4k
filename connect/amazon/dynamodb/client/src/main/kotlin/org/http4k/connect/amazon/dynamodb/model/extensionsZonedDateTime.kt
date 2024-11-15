package org.http4k.connect.amazon.dynamodb.model

import dev.forkhandles.values.Value
import dev.forkhandles.values.ValueFactory
import java.time.ZonedDateTime

@JvmName("valueZonedDateTime")
fun <VALUE : Value<ZonedDateTime>> Attribute.Companion.value(vf: ValueFactory<VALUE, ZonedDateTime>) =
    zonedDateTime().value(vf)
