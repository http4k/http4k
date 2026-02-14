package org.http4k.ai.a2a.server.protocol

import org.http4k.ai.a2a.protocol.messages.A2APushNotificationConfig

interface PushNotificationConfigs {
    fun set(request: A2APushNotificationConfig.Set.Request): A2APushNotificationConfig.Set.Response
    fun get(request: A2APushNotificationConfig.Get.Request): A2APushNotificationConfig.Get.Response?
    fun list(request: A2APushNotificationConfig.List.Request): A2APushNotificationConfig.List.Response
    fun delete(request: A2APushNotificationConfig.Delete.Request): A2APushNotificationConfig.Delete.Response?
}
