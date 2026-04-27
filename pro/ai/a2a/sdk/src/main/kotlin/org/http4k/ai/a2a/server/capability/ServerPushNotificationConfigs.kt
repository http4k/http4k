/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.ai.a2a.server.capability

import org.http4k.ai.a2a.model.PushNotificationConfigId
import org.http4k.ai.a2a.protocol.messages.A2APushNotificationConfig
import org.http4k.ai.a2a.model.TaskPushNotificationConfig
import org.http4k.ai.a2a.server.protocol.PushNotificationConfigs
import org.http4k.ai.a2a.server.storage.PushNotificationConfigStorage
import java.util.UUID

fun pushNotificationConfigs(
    storage: PushNotificationConfigStorage = PushNotificationConfigStorage.InMemory()
): PushNotificationConfigs = ServerPushNotificationConfigs(storage)

private class ServerPushNotificationConfigs(
    private val storage: PushNotificationConfigStorage
) : PushNotificationConfigs {

    override fun set(params: A2APushNotificationConfig.Set.Request.Params): A2APushNotificationConfig.Set.Response.Result {
        val configId = PushNotificationConfigId.of(UUID.randomUUID().toString())
        val taskConfig = TaskPushNotificationConfig(
            id = configId,
            taskId = params.taskId,
            pushNotificationConfig = params.pushNotificationConfig
        )
        storage.store(taskConfig)
        return A2APushNotificationConfig.Set.Response.Result(
            id = configId,
            taskId = params.taskId,
            pushNotificationConfig = params.pushNotificationConfig
        )
    }

    override fun get(params: A2APushNotificationConfig.Get.Request.Params): A2APushNotificationConfig.Get.Response.Result? {
        val config = storage.get(params.id) ?: return null
        return A2APushNotificationConfig.Get.Response.Result(
            id = config.id,
            taskId = config.taskId,
            pushNotificationConfig = config.pushNotificationConfig
        )
    }

    override fun list(params: A2APushNotificationConfig.List.Request.Params): A2APushNotificationConfig.List.Response.Result {
        val configs = storage.list(params.taskId)
        return A2APushNotificationConfig.List.Response.Result(configs = configs)
    }

    override fun delete(params: A2APushNotificationConfig.Delete.Request.Params): A2APushNotificationConfig.Delete.Response.Result? {
        val existing = storage.get(params.id) ?: return null
        storage.delete(params.id)
        return A2APushNotificationConfig.Delete.Response.Result(id = existing.id)
    }
}
