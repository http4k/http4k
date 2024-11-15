package org.http4k.connect.amazon.dynamodb.model

import dev.forkhandles.values.Value
import dev.forkhandles.values.ValueFactory

@JvmName("valueBoolean")
fun <VALUE : Value<Boolean>> Attribute.Companion.value(vf: ValueFactory<VALUE, Boolean>) = boolean().value(vf)

@JvmName("valueListBoolean")
fun <VALUE : Value<Boolean>> Attribute.Companion.list(vf: ValueFactory<VALUE, Boolean>) =
    vf.asList({ it.N?.toBoolean() }) { AttributeValue.Bool(it) }
