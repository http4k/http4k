package org.http4k.connect.amazon.dynamodb.model

import dev.forkhandles.values.Value
import dev.forkhandles.values.ValueFactory
import java.time.LocalDate

@JvmName("valueLocalDate")
fun <VALUE : Value<LocalDate>> Attribute.Companion.value(vf: ValueFactory<VALUE, LocalDate>) =
    localDate().value(vf)
