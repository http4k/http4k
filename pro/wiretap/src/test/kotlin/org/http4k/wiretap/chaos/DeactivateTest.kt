/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.wiretap.chaos

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.http4k.chaos.ChaosEngine
import org.http4k.core.Method.POST
import org.http4k.core.Request
import org.http4k.testing.Approver
import org.http4k.wiretap.HttpWiretapFunctionContract
import org.http4k.wiretap.McpWiretapFunctionContract
import org.junit.jupiter.api.Test

class DeactivateTest : HttpWiretapFunctionContract, McpWiretapFunctionContract {

    override val toolName = "chaos_deactivate"

    private val inboundChaos = ChaosEngine()
    private val outboundChaos = ChaosEngine()

    override val function = Deactivate(inboundChaos, outboundChaos)

    @Test
    fun `http disables inbound chaos`(approver: Approver) {
        inboundChaos.enable()
        assertThat(inboundChaos.isEnabled(), equalTo(true))

        approver.assertApproved(
            httpClient()(
                Request(POST, "/Inbound/deactivate")
            )
        )

        assertThat(inboundChaos.isEnabled(), equalTo(false))
    }

    @Test
    fun `mcp disables inbound chaos`(approver: Approver) {
        inboundChaos.enable()
        assertThat(inboundChaos.isEnabled(), equalTo(true))

        approver.assertToolResponse(mapOf("direction" to "Inbound"))

        assertThat(inboundChaos.isEnabled(), equalTo(false))
    }
}
