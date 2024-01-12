package org.http4k.webhook

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.http4k.core.Body
import org.http4k.webhook.SignatureIdentifier.v1
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class HmacSha256Test {

    private val webhookId = WebhookId.of("123")
    private val scheme = HmacSha256 { "secretkey" }
    private val timestamp = WebhookTimestamp.of(123)
    private val payload = Body("helloworld")

    @Test
    fun `can sign and verify`() {
        val signature = scheme.Signer(webhookId, timestamp, payload)
        assertThat(
            signature,
            equalTo(WebhookSignature.of(v1, SignedPayload.of("5dFLJKdpOXy5zSGQc8N5SgpF6ABfQXeeVba57De/KU8=")))
        )
        assertTrue(scheme.Verifier(webhookId, timestamp, signature, payload))
        assertFalse(scheme.Verifier(webhookId, timestamp, WebhookSignature.of(v1, SignedPayload.of("bogus")), payload))
    }
}
