package org.http4k.connect.amazon.dynamodb.model

import dev.forkhandles.values.Value
import dev.forkhandles.values.ValueFactory
import java.time.Instant

@JvmName("valueInstant")
fun <VALUE : Value<Instant>> Attribute.Companion.value(vf: ValueFactory<VALUE, Instant>) = instant().value(vf)
