package org.http4k.contract.ui

import org.http4k.core.Method.GET
import org.http4k.core.Request
import org.http4k.core.Uri
import org.http4k.testing.ApprovalTest
import org.http4k.testing.Approver
import org.http4k.testing.HtmlApprovalTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(ApprovalTest::class)
class SwaggerUiTest {

    @Test
    fun `can serve swagger ui`(approver: Approver) {
        val handler = swaggerUi(
            Uri.of("/spec"),
            title = "Cat Shelter",
            displayOperationId = true,
            requestSnippetsEnabled = true
        )
        approver.assertApproved(handler(Request(GET, "")))
    }

    @Test
    fun `can server swagger initializer`(approver: Approver) {
        val handler = swaggerUi(
            Uri.of("/spec"),
            title = "Cat Shelter",
            displayOperationId = true,
            requestSnippetsEnabled = true
        )

        approver.assertApproved(handler(Request(GET, "swagger-initializer.js")))
    }

    @Test
    fun `can serve swagger oauth2 redirect`(approver: Approver) {
        val handler = swaggerUi(
            Uri.of("/spec"),
            title = "Cat Shelter",
            displayOperationId = true,
            requestSnippetsEnabled = true
        )
        approver.assertApproved(handler(Request(GET, "oauth2-redirect.html")))
    }
}
