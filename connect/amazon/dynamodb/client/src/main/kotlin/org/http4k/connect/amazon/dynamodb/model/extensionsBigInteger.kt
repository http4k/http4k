package org.http4k.connect.amazon.dynamodb.model

import dev.forkhandles.values.Value
import dev.forkhandles.values.ValueFactory
import java.math.BigInteger

@JvmName("valueBigInteger")
fun <VALUE : Value<BigInteger>> Attribute.Companion.value(vf: ValueFactory<VALUE, BigInteger>) =
    bigInteger().value(vf)

@JvmName("valueListBigInteger")
fun <VALUE : Value<BigInteger>> Attribute.Companion.list(vf: ValueFactory<VALUE, BigInteger>) =
    vf.asList({ it.N?.toBigInteger() }) { AttributeValue.Num(it) }

@JvmName("valueSetBigInteger")
fun <VALUE : Value<BigInteger>> Attribute.Companion.bigIntegers(vf: ValueFactory<VALUE, BigInteger>) =
    bigIntegers().asSet(vf)
