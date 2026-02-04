package org.http4k.ai.mcp.protocol

import org.http4k.ai.mcp.util.McpJson
import org.http4k.core.ContentType.Companion.APPLICATION_JSON
import org.http4k.testing.Approver
import org.http4k.testing.JsonApprovalTest
import org.http4k.testing.assertApproved
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(JsonApprovalTest::class)
class CapabilitiesExtensionsTest {

    @Test
    fun `ClientCapabilities with extensions`(approver: Approver) {
        val extensions = mapOf(
            "io.modelcontextprotocol/ui" to mapOf(
                "mimeTypes" to listOf("text/html;profile=mcp-app")
            )
        )
        val capabilities = ClientCapabilities().copy(extensions = extensions)

        approver.assertApproved(McpJson.asFormatString(capabilities), APPLICATION_JSON)
    }

    @Test
    fun `ServerCapabilities with extensions`(approver: Approver) {
        val extensions = mapOf(
            "io.modelcontextprotocol/ui" to mapOf(
                "mimeTypes" to listOf("text/html;profile=mcp-app")
            )
        )
        val capabilities = ServerCapabilities().copy(extensions = extensions)

        approver.assertApproved(McpJson.asFormatString(capabilities), APPLICATION_JSON)
    }
}
