package org.http4k.connect.amazon.dynamodb.model

import dev.forkhandles.values.Value
import dev.forkhandles.values.ValueFactory
import java.math.BigDecimal

@JvmName("valueBigDecimal")
fun <VALUE : Value<BigDecimal>> Attribute.Companion.value(vf: ValueFactory<VALUE, BigDecimal>) =
    Attribute.bigDecimal().value(vf)

@JvmName("valueListBigDecimal")
fun <VALUE : Value<BigDecimal>> Attribute.Companion.list(vf: ValueFactory<VALUE, BigDecimal>) =
    vf.asList({ it.N?.toBigDecimal() }) { AttributeValue.Num(it) }

@JvmName("valueSetBigDecimal")
fun <VALUE : Value<BigDecimal>> Attribute.Companion.bigDecimals(vf: ValueFactory<VALUE, BigDecimal>) =
    bigDecimals().asSet(vf)
