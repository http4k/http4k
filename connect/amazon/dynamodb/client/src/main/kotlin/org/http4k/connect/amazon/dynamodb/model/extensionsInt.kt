package org.http4k.connect.amazon.dynamodb.model

import dev.forkhandles.values.Value
import dev.forkhandles.values.ValueFactory

@JvmName("valueInt")
fun <VALUE : Value<Int>> Attribute.Companion.value(vf: ValueFactory<VALUE, Int>) = Attribute.int().value(vf)

@JvmName("valueListInt")
fun <VALUE : Value<Int>> Attribute.Companion.list(vf: ValueFactory<VALUE, Int>) =
    vf.asList({ it.N?.toInt() }) { AttributeValue.Num(it) }

@JvmName("valueSetInt")
fun <VALUE : Value<Int>> Attribute.Companion.ints(vf: ValueFactory<VALUE, Int>) = ints().asSet(vf)
