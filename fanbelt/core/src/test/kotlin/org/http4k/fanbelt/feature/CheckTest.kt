/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.fanbelt.feature

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import dev.forkhandles.values.IntValue
import dev.forkhandles.values.IntValueFactory
import dev.forkhandles.values.StringValue
import dev.forkhandles.values.ValueFactory
import dev.forkhandles.values.length
import org.http4k.fanbelt.feature.Outcome.Rejected
import org.http4k.fanbelt.input.FieldName
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
    fun `an enum is checked by name, ignoring case and surrounding space, and anything else is an error on the field`() {
        assertThat(field.check<Colour>(" green ", "Pick red or green"), equalTo(Valid(Colour.Green)))
        assertThat(field.check<Colour>("blue", "Pick red or green"), equalTo(Rejected(mapOf(field to "Pick red or green"))))
    }

    @Test
    fun `a typed value is checked against its value type without parsing`() {
        assertThat(quantity.check(Quantity, 3, "Between 1 and 10"), equalTo(Valid(Quantity.of(3))))
        assertThat(quantity.check(Quantity, 11, "Between 1 and 10"), equalTo(Rejected(mapOf(quantity to "Between 1 and 10"))))
    }

    @Test
    fun `text goes through the value type's own parsing, so a title that trims is checked trimmed`() {
        assertThat(title.check(Title, "  hi  ", "Up to 5 characters"), equalTo(Valid(Title.of("hi"))))
        assertThat(title.check(Title, "   ", "Up to 5 characters"), equalTo(Rejected(mapOf(title to "Up to 5 characters"))))
    }
}
