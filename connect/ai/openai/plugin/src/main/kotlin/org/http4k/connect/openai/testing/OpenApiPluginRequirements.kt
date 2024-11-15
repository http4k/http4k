package org.http4k.connect.openai.testing

import com.natpryce.hamkrest.assertion.assertThat
import org.http4k.core.HttpHandler
import org.http4k.core.Method
import org.http4k.core.Request
import org.http4k.core.Status
import org.http4k.testing.Approver
import org.http4k.testing.JsonApprovalTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

/**
 * This defines the minimum set of test cases that an OpenAPI plugin should support.
 */
@ExtendWith(JsonApprovalTest::class)
interface OpenApiPluginRequirements {
    val openAiPlugin: HttpHandler

    @Test
    fun `serves manifest`(approver: Approver) {
        approver.assertApproved(openAiPlugin(Request(Method.GET, "/.well-known/ai-plugin.json")))
    }

    @Test
    fun `serves openapi`(approver: Approver) {
        approver.assertApproved(openAiPlugin(Request(Method.GET, "/openapi.json")))
    }

    @Test
    fun `serves logo`(approver: Approver) {
        assertThat(
            openAiPlugin(Request(Method.GET, "/logo.png")).status,
            com.natpryce.hamkrest.equalTo(Status.OK)
        )
    }
}
