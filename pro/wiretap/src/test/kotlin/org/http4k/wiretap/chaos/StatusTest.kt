/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.wiretap.chaos

import org.http4k.chaos.ChaosEngine
import org.http4k.core.Method.GET
import org.http4k.core.Request
import org.http4k.testing.Approver
import org.http4k.wiretap.HttpWiretapFunctionContract
import org.http4k.wiretap.McpWiretapFunctionContract
import org.junit.jupiter.api.Test

class StatusTest : HttpWiretapFunctionContract, McpWiretapFunctionContract {

    override val toolName = "chaos_status"

    private val inboundChaos = ChaosEngine()
    private val outboundChaos = ChaosEngine()

    override val function = Status(inboundChaos, outboundChaos)

    @Test
    fun `http returns inactive status`(approver: Approver) {
        approver.assertApproved(
            httpClient()(Request(GET, "/status"))
        )
    }

    @Test
    fun `http returns active status`(approver: Approver) {
        inboundChaos.enable()

        approver.assertApproved(
            httpClient()(Request(GET, "/status"))
        )
    }

    @Test
    fun `mcp returns inactive status`(approver: Approver) {
        approver.assertToolResponse(emptyMap())
    }

    @Test
    fun `mcp returns active status`(approver: Approver) {
        inboundChaos.enable()
        approver.assertToolResponse(emptyMap())
    }
}
