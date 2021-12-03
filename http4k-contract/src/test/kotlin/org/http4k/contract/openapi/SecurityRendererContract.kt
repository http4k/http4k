package org.http4k.contract.openapi

import com.fasterxml.jackson.databind.JsonNode
import org.http4k.contract.security.Security
import org.http4k.core.Body
import org.http4k.core.Response
import org.http4k.core.Status.Companion.OK
import org.http4k.core.with
import org.http4k.format.Jackson
import org.http4k.format.Jackson.json
import org.http4k.testing.Approver
import org.http4k.testing.JsonApprovalTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(JsonApprovalTest::class)
interface SecurityRendererContract {

    val security: Security
    val renderer: SecurityRenderer

    @Test
    fun ref(approver: Approver) {
        approver.assertSecurityRenders(renderer.ref(security))
    }

    @Test
    fun full(approver: Approver) {
        approver.assertSecurityRenders(renderer.full(security))
    }
}

private fun Approver.assertSecurityRenders(function: Render<JsonNode>?) {
    val lens = Body.json().toLens()
    assertApproved(Response(OK).with(lens of function?.invoke(Jackson)!!))
}
