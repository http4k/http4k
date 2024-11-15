package org.http4k.connect.amazon.dynamodb.model

import dev.forkhandles.values.Value
import dev.forkhandles.values.ValueFactory

@JvmName("valueDouble")
fun <VALUE : Value<Double>> Attribute.Companion.value(vf: ValueFactory<VALUE, Double>) = double().value(vf)

@JvmName("valueListDouble")
fun <VALUE : Value<Double>> Attribute.Companion.list(vf: ValueFactory<VALUE, Double>) =
    vf.asList({ it.N?.toDouble() }) { AttributeValue.Num(it) }

@JvmName("valueSetDouble")
fun <VALUE : Value<Double>> Attribute.Companion.doubles(vf: ValueFactory<VALUE, Double>) = doubles().asSet(vf)
