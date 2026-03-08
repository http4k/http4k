/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.ai.mcp.protocol

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.greaterThan
import org.http4k.ai.mcp.protocol.ProtocolVersion.Companion.LATEST_VERSION
import org.http4k.ai.mcp.protocol.ProtocolVersion.Companion.PUBLISHED
import org.junit.jupiter.api.Test

class ProtocolVersionTest {

    @Test
    fun `can compare`() {
        assertThat(LATEST_VERSION, greaterThan(PUBLISHED.min()))
    }
}
