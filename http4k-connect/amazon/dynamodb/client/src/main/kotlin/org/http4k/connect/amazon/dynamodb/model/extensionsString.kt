package org.http4k.connect.amazon.dynamodb.model

import dev.forkhandles.values.Value
import dev.forkhandles.values.ValueFactory

@JvmName("valueString")
fun <VALUE : Value<String>> Attribute.Companion.value(vf: ValueFactory<VALUE, String>) = string().value(vf)

fun <VALUE : Value<String>> Attribute.Companion.strings(vf: ValueFactory<VALUE, String>) = set(vf)
