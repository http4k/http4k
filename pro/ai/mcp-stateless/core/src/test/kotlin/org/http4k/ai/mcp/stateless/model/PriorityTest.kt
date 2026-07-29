/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.ai.mcp.stateless.model

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import com.natpryce.hamkrest.isA
import dev.forkhandles.result4k.Failure
import dev.forkhandles.values.ofResult4k
import org.junit.jupiter.api.Test

class PriorityTest {
    @Test
    fun `is bounded to 0 to 1 inclusive`() {
        assertThat(Priority.of(0.0).value, equalTo(0.0))
        assertThat(Priority.of(1.0).value, equalTo(1.0))
        assertThat(Priority.ofResult4k(-0.01), isA<Failure<Exception>>())
        assertThat(Priority.ofResult4k(1.01), isA<Failure<Exception>>())
    }
}
