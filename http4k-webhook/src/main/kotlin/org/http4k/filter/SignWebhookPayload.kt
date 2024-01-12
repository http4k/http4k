package org.http4k.filter

import org.http4k.core.Filter
import org.http4k.core.Request
import org.http4k.core.with
import org.http4k.lens.Header
import org.http4k.webhook.WEBHOOK_ID
import org.http4k.webhook.WEBHOOK_SIGNATURE
import org.http4k.webhook.WEBHOOK_TIMESTAMP
import org.http4k.webhook.WebhookId
import org.http4k.webhook.WebhookSigner
import org.http4k.webhook.WebhookTimestamp
import java.time.Clock
import java.util.UUID

fun ClientFilters.SignWebhookPayload(
    signer: WebhookSigner,
    clock: Clock = Clock.systemUTC(),
    idGenerator: (Request) -> WebhookId = { WebhookId.of(UUID.randomUUID().toString()) }
) = Filter { next ->
    {
        val id = idGenerator(it)
        val timestamp = WebhookTimestamp.of(clock.instant())
        val signature = signer(id, timestamp, it.body)
        next(
            it.with(
                Header.WEBHOOK_ID of id,
                Header.WEBHOOK_SIGNATURE of signature,
                Header.WEBHOOK_TIMESTAMP of timestamp
            )
        )
    }
}
