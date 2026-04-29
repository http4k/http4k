package org.http4k.contract.openapi.v3

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.http4k.contract.bindWebhook
import org.http4k.contract.contract
import org.http4k.contract.meta
import org.http4k.contract.openapi.ApiInfo
import org.http4k.contract.openapi.OpenAPIJackson
import org.http4k.contract.webhook
import org.http4k.core.Body
import org.http4k.core.Method.GET
import org.http4k.core.Method.POST
import org.http4k.core.Request
import org.http4k.core.Uri
import org.http4k.format.Jackson.auto
import org.http4k.routing.bind
import org.junit.jupiter.api.Test

class OpenApi3WebhookSchemaTest {
    data class WebhookOnlyPayload(val message: String, val count: Int)

    @Test
    fun `webhook body schemas are included in components schemas`() {
        val openApi3 = OpenApi3(
            ApiInfo("title", "1.0"),
            OpenAPIJackson,
            servers = listOf(ApiServer(Uri.of("http://localhost:8000")))
        )

        val router = "/" bind contract {
            renderer = openApi3
            webhook("payloadHook") {
                "/payload" meta {
                    receiving(Body.auto<WebhookOnlyPayload>().toLens() to WebhookOnlyPayload("hi", 1))
                } bindWebhook POST
            }
        }

        val description = router(Request(GET, "/"))
        val node = OpenAPIJackson.parse(description.bodyString())
        val schemas = node.path("components").path("schemas")

        assertThat(schemas.has("WebhookOnlyPayload"), equalTo(true))
    }
}
