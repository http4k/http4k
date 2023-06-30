package org.http4k.contract.ui

import org.http4k.core.Method
import org.http4k.core.Request
import org.http4k.testing.ApprovalTest
import org.http4k.testing.Approver
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(ApprovalTest::class)
class SwaggerUiConfigTest {
    @Test
    fun `serve minimal config`(approver: Approver) {
        val handler = swaggerUiLite()
        approver.assertApproved(handler(Request(Method.GET, "swagger-initializer.js")))
    }

    @Test
    fun `serve fully customized config`(approver: Approver) {
        val handler = swaggerUiLite {
            url = "foo"
            domId = "myUI"
            queryConfigEnabled = true

            displayOperationId = true
            displayRequestDuration = true
            requestSnippetsEnabled = true
            tryItOutEnabled = true
            deepLinking = true

            oauth2RedirectUrl = "sendmethere"
            withCredentials = true

            persistAuthorization = true

            layout = "myLayout"
            presets += "customPreset"
        }
        approver.assertApproved(handler(Request(Method.GET, "swagger-initializer.js")))
    }
}
