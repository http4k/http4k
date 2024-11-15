package org.http4k.connect.mattermost

import org.http4k.connect.mattermost.action.TriggerWebhookPayload
import org.http4k.connect.storage.Storage
import org.http4k.core.Method.POST
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status.Companion.OK
import org.http4k.lens.Path
import org.http4k.routing.RoutingHttpHandler
import org.http4k.routing.bind

fun incomingWebhooks(payloads: Storage<List<TriggerWebhookPayload>>): RoutingHttpHandler {
    val keyLens = Path.of("key")
    val payloadLens = MattermostMoshi.autoBody<TriggerWebhookPayload>().toLens()

    return "/hooks/$keyLens" bind POST to { req: Request ->
        val key = keyLens(req)
        payloads[key] = (payloads[key] ?: emptyList()) + payloadLens(req)
        Response(OK).body("ok")
    }
}
