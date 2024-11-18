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

fun ServerFilters.VerifyWebhookSignature(
    verifier: WebhookSignatureVerifier,
    onFailure: HttpHandler = { Response(UNAUTHORIZED) }
) = Filter { next ->
    {
        val verified = try {
            verifier(Header.WEBHOOK_ID(it), Header.WEBHOOK_TIMESTAMP(it), Header.WEBHOOK_SIGNATURE(it), it.body)
        } catch (e: LensFailure) {
            false
        }
        if (verified) next(it)
        else onFailure(it)
    }
}
