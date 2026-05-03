/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.ai.a2a.server

import org.http4k.ai.a2a.MessageHandler
import org.http4k.ai.a2a.model.AgentCard
import org.http4k.ai.a2a.server.storage.PushNotificationConfigStorage
import org.http4k.ai.a2a.server.storage.TaskStorage
import org.http4k.routing.a2aJsonRpc
import org.http4k.server.PolyServerConfig
import org.http4k.server.asServer

/**
 * Convienience method to construct an A2a server from a MessageHandeler. Uses InMemory versions of the
 * Tasks and
 */
fun MessageHandler.asServer(
    cfg: PolyServerConfig,
    agentCard: AgentCard,
    tasks: TaskStorage = TaskStorage.InMemory(),
    pushNotifications: PushNotificationConfigStorage = PushNotificationConfigStorage.InMemory(),
    subscriptions: TaskSubscriptions = TaskSubscriptions.InMemory(),

    rpcPath: String = "/",
) = a2aJsonRpc(agentCard, this, tasks, subscriptions, pushNotifications, rpcPath).asServer(cfg)
