package org.http4k.wiretap.client

import org.http4k.core.Method.GET
import org.http4k.core.Method.POST
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status.Companion.OK
import org.http4k.core.Uri
import org.http4k.testing.Approver
import org.http4k.wiretap.HttpWiretapFunctionContract
import org.http4k.wiretap.McpWiretapFunctionContract
import org.http4k.wiretap.domain.Direction.Inbound
import org.http4k.wiretap.util.Json.json
import org.junit.jupiter.api.Test
import java.time.Clock
import java.time.Instant.EPOCH
import java.time.ZoneOffset.UTC

class SendRequestTest : HttpWiretapFunctionContract, McpWiretapFunctionContract {

    override val toolName = "send_request"

    override val function = SendRequest(Clock.fixed(EPOCH, UTC), Inbound) {
        Response(OK).body("proxied")
    }

    @Test
    fun `http sends request through proxy`(approver: Approver) {
        approver.assertApproved(
            httpClient()(Request(POST, "/send").json(ClientRequest(method = GET, url = Uri.of("/test"))))
        )
    }

    @Test
    fun `mcp sends request through proxy`(approver: Approver) {
        approver.assertToolResponse(mapOf("method" to "GET", "url" to "/test"))
    }
}
