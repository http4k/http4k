/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.ai.a2a.server.protocol

import org.http4k.ai.a2a.protocol.messages.A2APushNotificationConfig

interface PushNotificationConfigs {
    fun set(params: A2APushNotificationConfig.Set.Request.Params): A2APushNotificationConfig.Set.Response.Result
    fun get(params: A2APushNotificationConfig.Get.Request.Params): A2APushNotificationConfig.Get.Response.Result?
    fun list(params: A2APushNotificationConfig.List.Request.Params): A2APushNotificationConfig.List.Response.Result
    fun delete(params: A2APushNotificationConfig.Delete.Request.Params): A2APushNotificationConfig.Delete.Response.Result?
}
