/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.wiretap.mcp_api

import org.http4k.testing.Approver
import org.junit.jupiter.api.Test

class DebugRequestPromptTest : McpPromptContract {

    override val promptName = "debug_request"
    override val prompt = DebugRequestPrompt()

    @Test
    fun `prompt returns debug steps with transaction id`(approver: Approver) {
        approver.assertPromptResponse(mapOf("transaction_id" to "42"))
    }
}
