package org.http4k.contract.ui

import org.http4k.core.Method
import org.http4k.core.Request
import org.http4k.testing.ApprovalTest
import org.http4k.testing.Approver
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(ApprovalTest::class)
class SwaggerUiLiteTest {
    @Test
    fun `can serve swagger ui`(approver: Approver) {
        val handler = swaggerUiLite {
            url = "spec"
            pageTitle = "Cat Shelter"
            displayOperationId = true
            requestSnippetsEnabled = true
        }
        approver.assertApproved(handler(Request(Method.GET, "")))
    }

    @Test
    fun `can serve swagger ui - customized dom_id`(approver: Approver) {
        val handler = swaggerUiLite {
            domId = "myui"
        }
        approver.assertApproved(handler(Request(Method.GET, "")))
    }

    @Test
    fun `can server swagger initializer`(approver: Approver) {
        val handler = swaggerUiLite {
            url = "spec"
            pageTitle = "Cat Shelter"
            displayOperationId = true
            requestSnippetsEnabled = true
        }
        approver.assertApproved(handler(Request(Method.GET, "swagger-initializer.js")))
    }

    @Test
    fun `can serve swagger oauth2 redirect`(approver: Approver) {
        val handler = swaggerUiLite {
            url = "spec"
            pageTitle = "Cat Shelter"
            displayOperationId = true
            requestSnippetsEnabled = true
        }
        approver.assertApproved(handler(Request(Method.GET, "oauth2-redirect.html")))
    }
}
