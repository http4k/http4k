package org.http4k.wiretap.mcp_api

import org.http4k.testing.Approver
import org.http4k.wiretap.mcp_api.McpPromptContract
import org.junit.jupiter.api.Test

class DebugRequestPromptTest : McpPromptContract {

    override val promptName = "debug_request"
    override val prompt = DebugRequestPrompt()

    @Test
    fun `prompt returns debug steps with transaction id`(approver: Approver) {
        approver.assertPromptResponse(mapOf("transaction_id" to "42"))
    }
}
