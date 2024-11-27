package org.http4k.connect.amazon.dynamodb.model

import dev.forkhandles.values.Value
import dev.forkhandles.values.ValueFactory

@JvmName("valueLong")
fun <VALUE : Value<Long>> Attribute.Companion.value(vf: ValueFactory<VALUE, Long>) = long().value(vf)

@JvmName("valueListLong")
fun <VALUE : Value<Long>> Attribute.Companion.list(vf: ValueFactory<VALUE, Long>) =
    vf.asList({ it.N?.toLong() }) { AttributeValue.Num(it) }

@JvmName("valueSetLong")
fun <VALUE : Value<Long>> Attribute.Companion.longs(vf: ValueFactory<VALUE, Long>) = longs().asSet(vf)
