package org.http4k.lens

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.http4k.connect.github.webhook.WebhookEventType
import org.http4k.core.Method.GET
import org.http4k.core.Request
import org.http4k.core.with
import org.junit.jupiter.api.Test
import java.util.UUID

class HeaderTests {

    @Test
    fun `roundtrip delivery`() {
        val randomUUID = UUID.randomUUID()
        val xGithubDelivery = Header.X_GITHUB_DELIVERY

        assertThat(
            xGithubDelivery(
                Request(GET, "").with(xGithubDelivery of randomUUID)
            ), equalTo(randomUUID)
        )
    }

    @Test
    fun `roundtrip signature`() {
        val xHubSignature256 = Header.X_HUB_SIGNATURE_256

        assertThat(
            xHubSignature256(
                Request(GET, "").with(xHubSignature256 of "randomUUID")
            ), equalTo("randomUUID")
        )
    }

    @Test
    fun `roundtrip event`() {
        val event = Header.X_GITHUB_EVENT

        assertThat(
            event(
                Request(GET, "").with(event of WebhookEventType.check_suite)
            ), equalTo(WebhookEventType.check_suite)
        )
    }
}
