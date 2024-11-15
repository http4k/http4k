package org.http4k.connect.amazon.dynamodb.model

import dev.forkhandles.values.Value
import dev.forkhandles.values.ValueFactory
import java.time.Duration

@JvmName("valueDuration")
fun <VALUE : Value<Duration>> Attribute.Companion.value(vf: ValueFactory<VALUE, Duration>) =
    duration().value(vf)
