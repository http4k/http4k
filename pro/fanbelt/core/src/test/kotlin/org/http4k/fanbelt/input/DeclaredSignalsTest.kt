/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.fanbelt.input

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class DeclaredSignalsTest {
    private data class Form(val title: String = "", val quantity: Int = 1, val urgent: Boolean = false)

    @Test
    fun `every field is declared with its default and its type, with an empty error each`() {
        assertEquals(
            """{"title":"","quantity":1,"urgent":false,"errors":{"title":"","quantity":"","urgent":""}}""",
            DeclaredSignals(Form()).json()
        )
    }

    @Test
    fun `a page without signals declares only an empty errors`() {
        assertEquals("""{"errors":{}}""", DeclaredSignals(null).json())
    }

    @Test
    fun `what the page painted rides along`() {
        assertEquals("""{"errors":{},"fanbeltPainted":{"list":"0123abcd"}}""", DeclaredSignals(null).json(mapOf("list" to "0123abcd")))
    }
}
