/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.ai.a2a.client.rest

import org.http4k.ai.a2a.MessageHandler
import org.http4k.ai.a2a.client.A2AClientContract
import org.http4k.ai.a2a.model.AgentCardProvider
import org.http4k.ai.a2a.server.storage.PushNotificationConfigStorage
import org.http4k.ai.a2a.server.storage.TaskStorage
import org.http4k.core.HttpHandler
import org.http4k.core.Uri
import org.http4k.routing.a2aRest

class RestA2AClientTest : A2AClientContract() {
    override fun serverFor(
        cards: AgentCardProvider,
        handler: MessageHandler,
        tasks: TaskStorage,
        pushNotifications: PushNotificationConfigStorage
    ) = a2aRest(cards, handler, tasks, pushNotifications)

    override fun clientFor(server: HttpHandler) =
        RestA2AClient(Uri.of("http://test"), server)
}
