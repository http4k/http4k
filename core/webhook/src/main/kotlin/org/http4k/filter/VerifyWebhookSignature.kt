package org.http4k.filter

import org.http4k.core.Filter
import org.http4k.core.HttpHandler
import org.http4k.core.Response
import org.http4k.core.Status.Companion.UNAUTHORIZED
import org.http4k.lens.Header
import org.http4k.lens.LensFailure
import org.http4k.lens.WEBHOOK_ID
import org.http4k.lens.WEBHOOK_SIGNATURE
import org.http4k.lens.WEBHOOK_TIMESTAMP
import org.http4k.webhook.signing.WebhookSignatureVerifier
import java.time.Clock
import java.time.Duration

fun ServerFilters.VerifyWebhookSignature(
    verifier: WebhookSignatureVerifier,
    onFailure: HttpHandler = { Response(UNAUTHORIZED) },
    clock: Clock = Clock.systemUTC(),
    tolerance: Duration = Duration.ofMinutes(5)
) = Filter { next ->
    {
        val verified = try {
            val timestamp = Header.WEBHOOK_TIMESTAMP(it)
            val withinTolerance = Duration.between(timestamp.asInstant(), clock.instant()).abs() <= tolerance
            withinTolerance && verifier(Header.WEBHOOK_ID(it), timestamp, Header.WEBHOOK_SIGNATURE(it), it.body)
        } catch (e: LensFailure) {
            false
        }
        if (verified) {
            next(it)
        } else {
            onFailure(it)
        }
    }
}
