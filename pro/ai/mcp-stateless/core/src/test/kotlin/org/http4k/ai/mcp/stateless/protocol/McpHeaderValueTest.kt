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

    @Test
    fun `an ascii-safe value is sent as a literal`() {
        assertThat(encodeMcpHeaderValue("Hello"), equalTo("Hello"))
        assertThat(encodeMcpHeaderValue(""), equalTo(""))
        assertThat(encodeMcpHeaderValue("a b~!"), equalTo("a b~!"))
    }

    @Test
    fun `a value outside the printable ascii range is wrapped`() {
        assertThat(encodeMcpHeaderValue("café"), equalTo("=?base64?Y2Fmw6k=?="))
    }

    @Test
    fun `a tab is wrapped even though a field value permits one`() {
        assertThat(encodeMcpHeaderValue("a\tb"), equalTo("=?base64?YQli?="))
    }

    @Test
    fun `surrounding whitespace is wrapped, because a header would lose it`() {
        assertThat(encodeMcpHeaderValue(" x"), equalTo("=?base64?IHg=?="))
        assertThat(encodeMcpHeaderValue("x "), equalTo("=?base64?eCA=?="))
    }

    @Test
    fun `whatever is encoded decodes back to itself`() {
        listOf("Hello", "", "café", "a\tb", " x", "x ", "=?base64?not-really?=").forEach {
            assertThat(decodeMcpHeaderValue(encodeMcpHeaderValue(it)), equalTo(it))
        }
    }
}
