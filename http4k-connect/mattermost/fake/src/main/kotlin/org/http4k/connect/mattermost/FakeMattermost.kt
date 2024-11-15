package org.http4k.connect.mattermost

import org.http4k.chaos.ChaoticHttpHandler
import org.http4k.chaos.start
import org.http4k.connect.mattermost.action.TriggerWebhookPayload
import org.http4k.connect.storage.InMemory
import org.http4k.connect.storage.Storage
import org.http4k.core.Uri
import org.http4k.routing.routes

class FakeMattermost(
    val triggerWebhookPayloads: Storage<List<TriggerWebhookPayload>> = Storage.InMemory(),
) : ChaoticHttpHandler() {
    override val app = routes(
        incomingWebhooks(triggerWebhookPayloads),
    )

    fun client() = Mattermost.Http(Uri.of(""), app)
}

fun main() {
    FakeMattermost().start()
}
