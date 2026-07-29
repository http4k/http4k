/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.ai.mcp.stateless.protocol

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.http4k.core.Method.POST
import org.http4k.core.Request
import org.http4k.lens.Header
import org.http4k.lens.stateless.MCP_NAME
import org.junit.jupiter.api.Test

class McpNameHeaderTest {

    private fun nameOf(value: String) = Header.MCP_NAME(Request(POST, "/mcp").header("Mcp-Name", value))

    @Test
    fun `a plain name is trimmed`() {
        assertThat(nameOf("  greet  "), equalTo("greet"))
    }

    @Test
    fun `an encoded name is decoded`() {
        assertThat(nameOf("=?base64?Z3JlZXQ=?="), equalTo("greet"))
    }

    @Test
    fun `a malformed sentinel is left alone so the comparison fails`() {
        assertThat(nameOf("=?base64?Z3JlZXQ?="), equalTo("=?base64?Z3JlZXQ?="))
    }
}
