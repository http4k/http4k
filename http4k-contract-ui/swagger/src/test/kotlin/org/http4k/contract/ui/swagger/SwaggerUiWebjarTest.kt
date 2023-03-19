package org.http4k.contract.ui.swagger

import com.natpryce.hamkrest.assertion.assertThat
import org.http4k.core.Method.GET
import org.http4k.core.Request
import org.http4k.core.Status
import org.http4k.core.Uri
import org.http4k.hamkrest.hasStatus
import org.http4k.testing.ApprovalTest
import org.http4k.testing.Approver
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(ApprovalTest::class)
class SwaggerUiWebjarTest {

    @Test
    fun `can serve swagger ui index`(approver: Approver) {
        val handler = swaggerUiWebjar(
            Uri.of("/spec"),
            title = "Cat Shelter",
            displayOperationId = true,
            requestSnippetsEnabled = true
        )
        approver.assertApproved(handler(Request(GET, "")))
    }

    @Test
    fun `can serve custom swagger ui initializer`(approver: Approver) {
        val handler = swaggerUiWebjar(
            Uri.of("/spec"),
            title = "Cat Shelter",
            displayOperationId = true,
            requestSnippetsEnabled = true
        )
        approver.assertApproved(handler(Request(GET, "swagger-initializer.js")))
    }

    @Test
    fun `can serve swagger oauth2 redirect`() {
        val handler = swaggerUiWebjar(
            Uri.of("/spec"),
            title = "Cat Shelter",
            displayOperationId = true,
            requestSnippetsEnabled = true
        )
        assertThat(handler(Request(GET, "oauth2-redirect.html")), hasStatus(Status.OK))
    }
}
