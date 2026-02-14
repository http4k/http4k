package org.http4k.ai.a2a.server.capability

import org.http4k.ai.a2a.model.PushNotificationConfigId
import org.http4k.ai.a2a.protocol.messages.A2APushNotificationConfig
import org.http4k.ai.a2a.model.TaskPushNotificationConfig
import org.http4k.ai.a2a.server.protocol.PushNotificationConfigs
import org.http4k.ai.a2a.server.storage.PushNotificationConfigStorage
import java.util.UUID

class ServerPushNotificationConfigs(
    private val storage: PushNotificationConfigStorage = PushNotificationConfigStorage.InMemory()
) : PushNotificationConfigs {

    override fun set(request: A2APushNotificationConfig.Set.Request): A2APushNotificationConfig.Set.Response {
        val configId = PushNotificationConfigId.of(UUID.randomUUID().toString())
        val taskConfig = TaskPushNotificationConfig(
            id = configId,
            taskId = request.taskId,
            pushNotificationConfig = request.pushNotificationConfig
        )
        storage.store(taskConfig)
        return A2APushNotificationConfig.Set.Response(
            id = configId,
            taskId = request.taskId,
            pushNotificationConfig = request.pushNotificationConfig
        )
    }

    override fun get(request: A2APushNotificationConfig.Get.Request): A2APushNotificationConfig.Get.Response? {
        val config = storage.get(request.id) ?: return null
        return A2APushNotificationConfig.Get.Response(
            id = config.id,
            taskId = config.taskId,
            pushNotificationConfig = config.pushNotificationConfig
        )
    }

    override fun list(request: A2APushNotificationConfig.List.Request): A2APushNotificationConfig.List.Response {
        val configs = storage.list(request.taskId)
        return A2APushNotificationConfig.List.Response(configs = configs)
    }

    override fun delete(request: A2APushNotificationConfig.Delete.Request): A2APushNotificationConfig.Delete.Response? {
        val existing = storage.get(request.id) ?: return null
        storage.delete(request.id)
        return A2APushNotificationConfig.Delete.Response(id = existing.id)
    }
}
