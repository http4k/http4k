package org.http4k.ai.mcp.protocol

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.greaterThan
import org.http4k.ai.mcp.protocol.ProtocolVersion.Companion.`2025-03-26`
import org.junit.jupiter.api.Test

class ProtocolVersionTest {

    @Test
    fun `can compare`() {
        assertThat(`2025-03-26`, greaterThan(ProtocolVersion.`2024-11-05`))
    }
}
