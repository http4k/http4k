/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.wiretap.domain

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.junit.jupiter.api.Test

class InMemoryTraceStoreTest : TraceStoreContract {
    override val store = TraceStore.InMemory()

    @Test
    fun `evicts oldest when maxSpans exceeded`() {
        val smallStore = TraceStore.InMemory(maxSpans = 2)

        smallStore.record(span("00000000000000000000000000000001", spanId = "1111111111111111", name = "first"))
        smallStore.record(span("00000000000000000000000000000001", spanId = "2222222222222222", name = "second"))
        smallStore.record(span("00000000000000000000000000000001", spanId = "3333333333333333", name = "third"))

        val spans = smallStore.get("00000000000000000000000000000001")
        assertThat(spans.size, equalTo(2))
        assertThat(spans.none { it.name == "first" }, equalTo(true))
    }
}
