/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.fanbelt.input

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.junit.jupiter.api.Test

class DeclaredSignalsTest {
    private data class Form(val title: String = "", val quantity: Int = 1, val urgent: Boolean = false)

    @Test
    fun `every field is declared with its default and its type, with an empty error each`() {
        assertThat(DeclaredSignals(Form()).json(), equalTo("""{"title":"","quantity":1,"urgent":false,"errors":{"title":"","quantity":"","urgent":""}}"""))
    }

    @Test
    fun `a page without signals declares only an empty errors`() {
        assertThat(DeclaredSignals(null).json(), equalTo("""{"errors":{}}"""))
    }

    @Test
    fun `what the page painted rides along`() {
        assertThat(DeclaredSignals(null).json(mapOf("list" to "0123abcd")), equalTo("""{"errors":{},"fanbeltPainted":{"list":"0123abcd"}}"""))
    }
}
