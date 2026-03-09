/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.wiretap.mcp_api

import org.http4k.testing.Approver
import org.junit.jupiter.api.Test

class AnalyzeTrafficPromptTest : McpPromptContract {

    override val promptName = "analyze_traffic"
    override val prompt = AnalyzeTrafficPrompt()

    @Test
    fun `prompt returns analysis steps`(approver: Approver) {
        approver.assertPromptResponse()
    }
}
