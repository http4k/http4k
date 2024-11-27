package org.http4k.connect.amazon.dynamodb.model

import dev.forkhandles.values.Value
import dev.forkhandles.values.ValueFactory

@JvmName("valueFloat")
fun <VALUE : Value<Float>> Attribute.Companion.value(vf: ValueFactory<VALUE, Float>) = float().value(vf)

@JvmName("valueListFloat")
fun <VALUE : Value<Float>> Attribute.Companion.list(vf: ValueFactory<VALUE, Float>) =
    vf.asList({ it.N?.toFloat() }) { AttributeValue.Num(it) }

@JvmName("valueSetFloat")
fun <VALUE : Value<Float>> Attribute.Companion.floats(vf: ValueFactory<VALUE, Float>) = floats().asSet(vf)
