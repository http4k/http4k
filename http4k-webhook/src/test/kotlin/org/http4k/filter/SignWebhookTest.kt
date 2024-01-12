package org.http4k.filter

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import dev.forkhandles.values.ZERO
import org.http4k.core.Method.*
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status.Companion.OK
import org.http4k.core.then
import org.http4k.core.with
import org.http4k.format.Jackson
import org.http4k.hamkrest.hasHeader
import org.http4k.hamkrest.hasStatus
import org.http4k.lens.Header
import org.http4k.lens.WEBHOOK_ID
import org.http4k.lens.WEBHOOK_SIGNATURE
import org.http4k.lens.WEBHOOK_TIMESTAMP
import org.http4k.util.FixedClock
import org.http4k.webhook.EventType
import org.http4k.webhook.signing.SignatureIdentifier
import org.http4k.webhook.signing.SignedPayload
import org.http4k.webhook.WebhookId
import org.http4k.webhook.WebhookPayload
import org.http4k.webhook.signing.WebhookSignature
import org.http4k.webhook.WebhookTimestamp
import org.junit.jupiter.api.Test

class SignWebhookTest {

    @Test
    fun `signing populates request`() {
        val webhookId = WebhookId.of("123")
        val signature = WebhookSignature.of(SignatureIdentifier.v1, SignedPayload.encode("payload"))
        val app = ClientFilters.SignWebhookPayload(
            { _, _, _ -> signature },
            Jackson,
            { webhookId }
        ).then {
            assertThat(it, hasHeader(Header.WEBHOOK_ID, equalTo(webhookId)))
            assertThat(it, hasHeader(Header.WEBHOOK_TIMESTAMP, equalTo(WebhookTimestamp.ZERO)))
            assertThat(it, hasHeader(Header.WEBHOOK_SIGNATURE, equalTo(signature)))
            Response(OK)
        }

        assertThat(app(Request(POST, "").body(
            Jackson.asFormatString(WebhookPayload(EventType.of("123"), WebhookTimestamp.of(0), "hello"))
        )), hasStatus(OK))
    }
}
