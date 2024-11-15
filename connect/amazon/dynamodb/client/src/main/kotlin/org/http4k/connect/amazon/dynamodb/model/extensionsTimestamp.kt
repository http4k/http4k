package org.http4k.connect.amazon.dynamodb.model

import dev.forkhandles.values.Value
import dev.forkhandles.values.ValueFactory
import org.http4k.connect.model.Timestamp

@JvmName("valueTimestamp")
fun <VALUE : Value<Timestamp>> Attribute.Companion.value(vf: ValueFactory<VALUE, Timestamp>) =
    timestamp().value(vf)
