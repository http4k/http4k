/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.fanbelt.input

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.containsSubstring
import org.http4k.core.Method.POST
import org.http4k.core.Request
import org.http4k.fanbelt.feature.Outcome.Rejected
import org.junit.jupiter.api.Test

class ErrorSignalsTest {
    @Test
    fun `a rejection clears the errors it no longer shows`() {
        val showingTitle = Request(POST, "/lots").body("""{"errors":{"title":"A title is needed"}}""")

        val body = ErrorSignals().rejected(showingTitle, Rejected(FieldName.of("amount"), "Bid more")).bodyString()

        assertThat(body, containsSubstring("""{"errors":{"title":"","amount":"Bid more"}}"""))
    }
}
