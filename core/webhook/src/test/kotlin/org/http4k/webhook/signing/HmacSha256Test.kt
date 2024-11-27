package org.http4k.webhook.signing

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.http4k.core.Body
import org.http4k.webhook.WebhookId
import org.http4k.webhook.WebhookTimestamp
import org.http4k.webhook.signing.SignatureIdentifier.v1
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class HmacSha256Test {

    private val webhookId = WebhookId.of("123")
    private val secret = HmacSha256SigningSecret.encode("A".repeat(24))
    private val timestamp = WebhookTimestamp.of(123)
    private val payload = Body("helloworld")

    @Test
    fun `signing secret validates`() {
        assertThrows<Exception> { HmacSha256SigningSecret.encode("A".repeat(23)) }
        HmacSha256SigningSecret.encode("A".repeat(24))
        HmacSha256SigningSecret.encode("A".repeat(64))
        assertThrows<Exception> { HmacSha256SigningSecret.encode("A".repeat(65)) }
    }
    
    @Test
    fun `can sign and verify`() {
        val signer = HmacSha256.Signer(secret)
        val verifier = HmacSha256.Verifier(secret)
        val signature = signer(webhookId, timestamp, payload)
        assertThat(
            signature,
            equalTo(WebhookSignature.of(v1, SignedPayload.of("OcbCYuZZFscUg29vzRqaNlkTWR/87qUI4JLNkd407FY=")))
        )
        assertTrue(verifier(webhookId, timestamp, signature, payload))
        assertFalse(verifier(webhookId, timestamp, WebhookSignature.of(v1, SignedPayload.of("bogus")), payload))
    }
}
