/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.fanbelt.feature

import dev.forkhandles.values.IntValue
import dev.forkhandles.values.IntValueFactory
import dev.forkhandles.values.StringValue
import dev.forkhandles.values.ValueFactory
import dev.forkhandles.values.length
import org.http4k.fanbelt.feature.Outcome.Rejected
import org.http4k.fanbelt.feature.Validated.Valid
import org.http4k.fanbelt.input.FieldName
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class CheckTest {
    private enum class Colour { Red, Green }

    private class Quantity private constructor(value: Int) : IntValue(value) {
        companion object : IntValueFactory<Quantity>(::Quantity, { it in 1..10 })
    }

    private class Title private constructor(value: String) : StringValue(value) {
        companion object : ValueFactory<Title, String>(::Title, (1..5).length, String::trim)
    }

    private val field = FieldName.of("colour")
    private val title = FieldName.of("title")
    private val quantity = FieldName.of("quantity")

    @Test
    fun `an enum is checked by name, ignoring case and surrounding space`() {
        assertEquals(Valid(Colour.Green), field.check(Colour.entries, " green ", "Pick red or green"))
    }

    @Test
    fun `anything else is an error on the field`() {
        assertEquals(Rejected(mapOf(field to "Pick red or green")), field.check(Colour.entries, "blue", "Pick red or green"))
    }

    @Test
    fun `a typed value is checked against its value type without parsing`() {
        assertEquals(Valid(Quantity.of(3)), quantity.check(Quantity, 3, "Between 1 and 10"))
        assertEquals(Rejected(mapOf(quantity to "Between 1 and 10")), quantity.check(Quantity, 11, "Between 1 and 10"))
    }

    @Test
    fun `text goes through the value type's own parsing, so a title that trims is checked trimmed`() {
        assertEquals(Valid(Title.of("hi")), title.check(Title, "  hi  ", "Up to 5 characters"))
        assertEquals(Rejected(mapOf(title to "Up to 5 characters")), title.check(Title, "   ", "Up to 5 characters"))
    }
}
