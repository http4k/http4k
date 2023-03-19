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

/**
 * var pageTitle: String? = null,
 *
 *     // core
 *     var url: String = "https://petstore.swagger.io/v2/swagger.json",
 *     var domId: String = "swagger-ui",
 *     var queryConfigEnabled: Boolean? = null,
 *
 *     // display
 *     var displayOperationId: Boolean? = null,
 *     var displayRequestDuration: Boolean? = null,
 *     var requestSnippetsEnabled: Boolean? = null,
 *     var tryItOutEnabled: Boolean? = null,
 *     var deepLinking: Boolean? = null,
 *
 *     // Network
 *     var oauth2RedirectUrl: String? = null,
 *     var withCredentials: Boolean? = null,
 *
 *     // Authorization
 *     var persistAuthorization: Boolean? = null,
 *
 *     // plugins
 *     var layout: String = "BaseLayout",
 *     var presets: List<String> = listOf("SwaggerUIBundle.presets.apis")
 */
