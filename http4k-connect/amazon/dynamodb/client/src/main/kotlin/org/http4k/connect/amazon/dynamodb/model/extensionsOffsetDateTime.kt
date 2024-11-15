package org.http4k.connect.amazon.dynamodb.model

import dev.forkhandles.values.Value
import dev.forkhandles.values.ValueFactory
import java.time.OffsetDateTime

@JvmName("valueOffsetDateTime")
fun <VALUE : Value<OffsetDateTime>> Attribute.Companion.value(vf: ValueFactory<VALUE, OffsetDateTime>) =
    offsetDateTime().value(vf)
