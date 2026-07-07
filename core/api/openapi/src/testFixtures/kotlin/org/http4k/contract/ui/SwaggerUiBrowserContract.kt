package org.http4k.contract.ui

import com.microsoft.playwright.assertions.PlaywrightAssertions
import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.http4k.contract.contract
import org.http4k.contract.meta
import org.http4k.contract.openapi.ApiInfo
import org.http4k.contract.openapi.v3.OpenApi3
import org.http4k.core.Method
import org.http4k.core.Response
import org.http4k.core.Status
import org.http4k.playwright.Http4kBrowser
import org.http4k.playwright.LaunchPlaywrightBrowser
import org.http4k.routing.RoutingHttpHandler
import org.http4k.routing.routes
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension

abstract class SwaggerUiBrowserContract(swaggerUi: (SwaggerUiConfig.() -> Unit) -> RoutingHttpHandler) {

    private val app = routes(
        contract {
            renderer = OpenApi3(ApiInfo("Cat Shelter", "1"))
            descriptionPath = "openapi.json"

            routes += "/cats" meta {
                operationId = "listCats"
            } bindContract Method.GET to { _ -> Response(Status.OK) }

            routes += "/cats" meta {
                operationId = "createCat"
            } bindContract Method.POST to { _ -> Response(Status.OK) }
        },
        swaggerUi {
            url = "openapi.json"
            displayOperationId = true
        }
    )

    @RegisterExtension
    val playwright = LaunchPlaywrightBrowser(app)

    @Test
    fun `can locate operation ids`(browser: Http4kBrowser) {
        with(browser.newPage()) {
            navigateHome()

            val summaries = locator(".opblock-summary-operation-id")

            PlaywrightAssertions.assertThat(summaries).hasCount(2)
            assertThat(summaries.allInnerTexts(), equalTo(listOf("listCats", "createCat")))
        }
    }
}
