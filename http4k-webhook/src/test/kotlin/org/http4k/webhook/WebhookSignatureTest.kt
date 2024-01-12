package org.http4k.webhook

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.http4k.webhook.SignatureIdentifier.v1
import org.junit.jupiter.api.Test

class WebhookSignatureTest {

    @Test
    fun `can roundtrip`() {
        val payload = SignedPayload.encode("foobar")
        val sig = WebhookSignature.of(v1, payload)
        assertThat(sig.identifier, equalTo(v1))
        assertThat(sig.payload, equalTo(payload))
    }
}
