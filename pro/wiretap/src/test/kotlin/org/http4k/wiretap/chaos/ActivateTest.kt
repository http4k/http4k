/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.wiretap.chaos

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.http4k.ai.mcp.apps.util.McpAppsJson.json
import org.http4k.chaos.ChaosEngine
import org.http4k.core.Method.POST
import org.http4k.core.Request
import org.http4k.testing.Approver
import org.http4k.wiretap.HttpWiretapFunctionContract
import org.http4k.wiretap.McpWiretapFunctionContract
import org.junit.jupiter.api.Test

class ActivateTest : HttpWiretapFunctionContract, McpWiretapFunctionContract {

    override val toolName = "chaos_activate"

    private val inboundChaos = ChaosEngine()
    private val outboundChaos = ChaosEngine()

    override val function = Activate(inboundChaos, outboundChaos)

    @Test
    fun `http default response`(approver: Approver) {
        // setup
        assertThat(inboundChaos.isEnabled(), equalTo(false))

        // make call and assert response
        approver.assertApproved(
            httpClient()(
                Request(POST, "/Inbound/activate")
                    .json(mapOf("direction" to "Inbound"))
            )
        )

        // check collaborators
        assertThat(inboundChaos.isEnabled(), equalTo(true))
    }

    @Test
    fun `mcp default response`(approver: Approver) {
        approver.assertToolResponse(mapOf("direction" to "Inbound"))
    }
}
