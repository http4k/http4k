/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.fanbelt.feature

import org.http4k.fanbelt.feature.Outcome.Rejected
import org.http4k.fanbelt.feature.Validated.Valid
import org.http4k.fanbelt.input.FieldName
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class CheckTest {
    private enum class Colour { Red, Green }

    private val field = FieldName.of("colour")

    @Test
    fun `an enum is checked by name, ignoring case and surrounding space`() {
        assertEquals(Valid(Colour.Green), field.check(Colour.entries, " green ", "Pick red or green"))
    }

    @Test
    fun `anything else is an error on the field`() {
        assertEquals(Rejected(mapOf(field to "Pick red or green")), field.check(Colour.entries, "blue", "Pick red or green"))
    }
}
