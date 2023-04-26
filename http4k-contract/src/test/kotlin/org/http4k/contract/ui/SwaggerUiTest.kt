package org.http4k.contract.ui

import org.http4k.core.Method.GET
import org.http4k.core.Request
import org.http4k.core.Uri
import org.http4k.testing.ApprovalTest
import org.http4k.testing.Approver
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(ApprovalTest::class)
class SwaggerUiTest {

    @Test
    fun `can serve swagger ui`(approver: Approver) {
        val handler = swaggerUiLite {
            pageTitle = "Cat Shelter"
            url = Uri.of("/spec").toString()
            displayOperationId = true
            displayRequestDuration = false
            requestSnippetsEnabled = true
            persistAuthorization = false
            queryConfigEnabled = false
            tryItOutEnabled = false
            deepLinking = true
            layout = "StandaloneLayout"
            presets += "SwaggerUIStandalonePreset"
        }
        approver.assertApproved(handler(Request(GET, "")))
    }

    @Test
    fun `can server swagger initializer`(approver: Approver) {
        val handler = swaggerUiLite {
            pageTitle = "Cat Shelter"
            url = Uri.of("/spec").toString()
            displayOperationId = true
            displayRequestDuration = false
            requestSnippetsEnabled = true
            persistAuthorization = false
            queryConfigEnabled = false
            tryItOutEnabled = false
            deepLinking = true
            layout = "StandaloneLayout"
            presets += "SwaggerUIStandalonePreset"
        }

        approver.assertApproved(handler(Request(GET, "swagger-initializer.js")))
    }

    @Test
    fun `can serve swagger oauth2 redirect`(approver: Approver) {
        val handler = swaggerUiLite {
            pageTitle = "Cat Shelter"
            url = Uri.of("/spec").toString()
            displayOperationId = true
            displayRequestDuration = false
            requestSnippetsEnabled = true
            persistAuthorization = false
            queryConfigEnabled = false
            tryItOutEnabled = false
            deepLinking = true
            layout = "StandaloneLayout"
            presets += "SwaggerUIStandalonePreset"
        }
        approver.assertApproved(handler(Request(GET, "oauth2-redirect.html")))
    }
}
