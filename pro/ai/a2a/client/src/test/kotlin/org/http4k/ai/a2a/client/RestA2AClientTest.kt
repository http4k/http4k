/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.ai.a2a.client

import org.http4k.ai.a2a.MessageHandler
import org.http4k.ai.a2a.model.AgentCardProvider
import org.http4k.ai.a2a.server.storage.PushNotificationConfigStorage
import org.http4k.ai.a2a.server.storage.TaskStorage
import org.http4k.core.Uri
import org.http4k.routing.a2aRest
import org.http4k.util.PortBasedTest

class RestA2AClientTest : A2AClientContract(), PortBasedTest {
    override fun serverFor(
        cards: AgentCardProvider,
        tasks: TaskStorage,
        pushNotifications: PushNotificationConfigStorage,
        handler: MessageHandler
    ) = a2aRest(cards, tasks, pushNotifications, messageHandler = handler)

    override fun clientFor(port: Int) =
        RestA2AClient(Uri.of("http://localhost:$port"))
}
