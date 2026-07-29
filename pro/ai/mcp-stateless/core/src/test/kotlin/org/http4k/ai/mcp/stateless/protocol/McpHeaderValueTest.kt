/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.ai.mcp.stateless.protocol

import com.natpryce.hamkrest.absent
import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.junit.jupiter.api.Test

class McpHeaderValueTest {

    @Test
    fun `a well formed sentinel is decoded`() {
        assertThat(decodeMcpHeaderValue("=?base64?SGVsbG8=?="), equalTo("Hello"))
        assertThat(decodeMcpHeaderValue("=?base64?Y2Fmw6k=?="), equalTo("café"))
    }

    @Test
    fun `a value without the full sentinel is a literal`() {
        assertThat(decodeMcpHeaderValue("Hello"), equalTo("Hello"))
        assertThat(decodeMcpHeaderValue("SGVsbG8="), equalTo("SGVsbG8="))
        assertThat(decodeMcpHeaderValue("=?base64?SGVsbG8="), equalTo("=?base64?SGVsbG8="))
    }

    @Test
    fun `a malformed sentinel payload is rejected rather than treated as a literal`() {
        assertThat(decodeMcpHeaderValue("=?base64?SGVsbG8?="), absent())
        assertThat(decodeMcpHeaderValue("=?base64?SGVs!!!bG8=?="), absent())
    }

    @Test
    fun `an empty payload decodes to empty`() {
        assertThat(decodeMcpHeaderValue("=?base64??="), equalTo(""))
    }
}
