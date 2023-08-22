package org.http4k.format

import dev.forkhandles.values.AbstractValue
import dev.forkhandles.values.BigDecimalValueFactory
import dev.forkhandles.values.BigIntegerValueFactory
import dev.forkhandles.values.BooleanValueFactory
import dev.forkhandles.values.DoubleValueFactory
import dev.forkhandles.values.FloatValueFactory
import dev.forkhandles.values.IntValueFactory
import dev.forkhandles.values.LocalDateTimeValueFactory
import dev.forkhandles.values.LongValueFactory
import dev.forkhandles.values.StringValueFactory
import org.http4k.util.mockReturnNull
import org.junit.jupiter.api.Test

class TV<T : Any>(value: T) : AbstractValue<T>(value)

class Values4kExtensionTest {
    @Test
    fun `type checks ok`() {
        val mapping = object : AutoMappingConfiguration<String> by mockReturnNull() {}

        mapping.value(IntValueFactory(::TV))
        mapping.value(LongValueFactory(::TV))
        mapping.value(DoubleValueFactory(::TV))
        mapping.value(FloatValueFactory(::TV))
        mapping.value(BigDecimalValueFactory(::TV))
        mapping.value(BigIntegerValueFactory(::TV))
        mapping.value(BooleanValueFactory(::TV))

        // default to text
        mapping.value(StringValueFactory(::TV))
        mapping.value(LocalDateTimeValueFactory(::TV))
    }
}
