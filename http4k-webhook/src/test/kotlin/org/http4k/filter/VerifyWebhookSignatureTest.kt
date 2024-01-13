package org.http4k.filter

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import dev.forkhandles.values.ZERO
import org.http4k.core.Method.GET
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status.Companion.I_M_A_TEAPOT
import org.http4k.core.Status.Companion.OK
import org.http4k.core.then
import org.http4k.core.with
import org.http4k.lens.Header
import org.http4k.lens.WEBHOOK_ID
import org.http4k.lens.WEBHOOK_SIGNATURE
import org.http4k.lens.WEBHOOK_TIMESTAMP
import org.http4k.webhook.WebhookId
import org.http4k.webhook.WebhookTimestamp
import org.http4k.webhook.signing.SignatureIdentifier
import org.http4k.webhook.signing.SignedPayload
import org.http4k.webhook.signing.WebhookSignature
import org.junit.jupiter.api.Test

class VerifyWebhookSignatureTest {

    @Test
    fun `verify ok signature`() {
        val app = ServerFilters.VerifyWebhookSignature({ _, _, _, _ ->
            true
        }, { Response(I_M_A_TEAPOT) })
            .then { Response(OK) }

        assertThat(
            app(
                Request(GET, "")
                    .with(
                        Header.WEBHOOK_ID of WebhookId.of("123"),
                        Header.WEBHOOK_SIGNATURE of WebhookSignature.of(
                            SignatureIdentifier.v1,
                            SignedPayload.encode("foobar")
                        ),
                        Header.WEBHOOK_TIMESTAMP of WebhookTimestamp.ZERO
                    )
            ), equalTo(Response(OK))
        )
    }

    @Test
    fun `don't verify bad signature`() {
        val app = ServerFilters.VerifyWebhookSignature({ _, _, _, _ ->
            false
        }, { Response(I_M_A_TEAPOT) })
            .then { Response(OK) }

        assertThat(
            app(
                Request(GET, "")
                    .with(
                        Header.WEBHOOK_ID of WebhookId.of("123"),
                        Header.WEBHOOK_SIGNATURE of WebhookSignature.of(
                            SignatureIdentifier.v1,
                            SignedPayload.encode("foobar")
                        ),
                        Header.WEBHOOK_TIMESTAMP of WebhookTimestamp.ZERO
                    )
            ), equalTo(Response(I_M_A_TEAPOT))
        )
    }

    @Test
    fun `missing signature is bad`() {
        val app = ServerFilters.VerifyWebhookSignature({ _, _, _, _ -> error("") }, { Response(I_M_A_TEAPOT) })
            .then { error("") }

        assertThat(
            app(
                Request(GET, "")
                    .with(
                        Header.WEBHOOK_ID of WebhookId.of("123"),
                        Header.WEBHOOK_TIMESTAMP of WebhookTimestamp.ZERO
                    )
            ), equalTo(Response(I_M_A_TEAPOT))
        )
    }
}
