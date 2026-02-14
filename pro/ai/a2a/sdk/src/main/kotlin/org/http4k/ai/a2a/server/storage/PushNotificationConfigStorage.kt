package org.http4k.ai.a2a.server.storage

import org.http4k.ai.a2a.model.PushNotificationConfigId
import org.http4k.ai.a2a.model.TaskId
import org.http4k.ai.a2a.model.TaskPushNotificationConfig
import java.util.concurrent.ConcurrentHashMap

interface PushNotificationConfigStorage {
    fun store(config: TaskPushNotificationConfig)
    fun get(id: PushNotificationConfigId): TaskPushNotificationConfig?
    fun delete(id: PushNotificationConfigId)
    fun list(taskId: TaskId): List<TaskPushNotificationConfig>

    companion object {
        fun InMemory() = object : PushNotificationConfigStorage {
            private val configs = ConcurrentHashMap<PushNotificationConfigId, TaskPushNotificationConfig>()

            override fun store(config: TaskPushNotificationConfig) {
                configs[config.id] = config
            }

            override fun get(id: PushNotificationConfigId) = configs[id]

            override fun delete(id: PushNotificationConfigId) {
                configs.remove(id)
            }

            override fun list(taskId: TaskId) = configs.values.filter { it.taskId == taskId }
        }
    }
}
