package org.http4k.connect.amazon.dynamodb.model

import dev.forkhandles.values.Value
import dev.forkhandles.values.ValueFactory
import java.time.LocalTime

@JvmName("valueLocalTime")
fun <VALUE : Value<LocalTime>> Attribute.Companion.value(vf: ValueFactory<VALUE, LocalTime>) =
    Attribute.localTime().value(vf)
