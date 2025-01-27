package org.http4k.contract.ui.swagger

import com.natpryce.hamkrest.and
import com.natpryce.hamkrest.assertion.assertThat
import org.http4k.core.Method.GET
import org.http4k.core.Request
import org.http4k.core.Status
import org.http4k.hamkrest.hasHeader
import org.http4k.hamkrest.hasStatus
import org.http4k.routing.bind
import org.http4k.routing.routes
import org.http4k.testing.ApprovalTest
import org.http4k.testing.Approver
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(ApprovalTest::class)
class SwaggerUiWebjarTest {

    @Test // regression test for issue 880
    fun `base path redirects to index`() {
        val handler = routes(
            "ui" bind swaggerUiWebjar {
                domId = "myui"
            }
        )
        assertThat(handler(Request(GET, "ui")), hasStatus(Status.FOUND).and(hasHeader("Location", "ui/index.html")))
    }

    @Test
    fun `can serve swagger ui index`(approver: Approver) {
        val handler = swaggerUiWebjar {
            url = "spec"
            pageTitle = "Cat Shelter"
            displayOperationId = true
            requestSnippetsEnabled = true
        }
        approver.assertApproved(handler(Request(GET, "index.html")))
    }

    @Test
    fun `can serve custom swagger ui initializer`(approver: Approver) {
        val handler = swaggerUiWebjar {
            url = "spec"
            pageTitle = "Cat Shelter"
            displayOperationId = true
            requestSnippetsEnabled = true
        }
        approver.assertApproved(handler(Request(GET, "swagger-initializer.js")))
    }

    @Test
    fun `can serve swagger oauth2 redirect`(approver: Approver) {
        val handler = swaggerUiWebjar {
            url = "spec"
            pageTitle = "Cat Shelter"
            displayOperationId = true
            requestSnippetsEnabled = true
        }
        approver.assertApproved(handler(Request(GET, "oauth2-redirect.html")))
    }
}
