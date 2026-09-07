/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.postbox

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import com.natpryce.hamkrest.throws
import org.junit.jupiter.api.Test

class RequestIdTest {

    @Test
    fun `accepts a request id of maximum length`() {
        assertThat(RequestId.of("a".repeat(RequestId.MAX_LENGTH)).value, equalTo("a".repeat(RequestId.MAX_LENGTH)))
    }

    @Test
    fun `rejects a request id longer than the maximum length`() {
        assertThat({ RequestId.of("a".repeat(RequestId.MAX_LENGTH + 1)) }, throws<IllegalArgumentException>())
    }
}
