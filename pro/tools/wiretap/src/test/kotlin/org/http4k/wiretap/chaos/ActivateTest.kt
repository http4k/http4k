package org.http4k.wiretap.chaos

import org.http4k.ai.mcp.apps.util.McpAppsJson.json
import org.http4k.chaos.ChaosEngine
import org.http4k.core.Method
import org.http4k.core.Method.*
import org.http4k.core.Request
import org.http4k.filter.debug
import org.http4k.testing.Approver
import org.http4k.wiretap.HttpWiretapFunctionContract
import org.http4k.wiretap.McpWiretapFunctionContract
import org.junit.jupiter.api.Test
import kotlin.collections.mapOf

class ActivateTest : HttpWiretapFunctionContract, McpWiretapFunctionContract {

    override val toolName = "chaos_activate"

    private val inboundChaos = ChaosEngine()
    private val outboundChaos = ChaosEngine()

    override val function = Activate(inboundChaos, outboundChaos)

    @Test
    fun `http default response`(approver: Approver) {
        approver.assertApproved(
            httpClient()(
                Request(POST, "/Inbound/activate")
                    .json(mapOf("direction" to "Inbound"))
            )
        )
    }

    @Test
    fun `mcp default response`(approver: Approver) {
        approver.assertToolResponse(mapOf("direction" to "Inbound"))
    }
}
