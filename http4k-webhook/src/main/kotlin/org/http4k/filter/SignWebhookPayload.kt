package org.http4k.filter

import org.http4k.core.Filter
import org.http4k.core.Request
import org.http4k.core.with
import org.http4k.format.AutoMarshalling
import org.http4k.lens.Header
import org.http4k.lens.WEBHOOK_ID
import org.http4k.lens.WEBHOOK_SIGNATURE
import org.http4k.lens.WEBHOOK_TIMESTAMP
import org.http4k.webhook.WebhookId
import org.http4k.webhook.WebhookPayload
import org.http4k.webhook.signing.WebhookSigner
import java.util.UUID

fun ClientFilters.SignWebhookPayload(
    signer: WebhookSigner,
    autoMarshalling: AutoMarshalling,
    idGenerator: (Request) -> WebhookId = { WebhookId.of(UUID.randomUUID().toString()) }
) = Filter { next ->
    {
        val id = idGenerator(it)
        val webhook = autoMarshalling.asA<WebhookPayload<Any>>(it.bodyString())
        val signature = signer(id, webhook.timestamp, it.body)
        next(
            it.with(
                Header.WEBHOOK_ID of id,
                Header.WEBHOOK_SIGNATURE of signature,
                Header.WEBHOOK_TIMESTAMP of webhook.timestamp
            )
        )
    }
}
