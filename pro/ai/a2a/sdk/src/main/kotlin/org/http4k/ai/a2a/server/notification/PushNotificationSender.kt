/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.ai.a2a.server.notification

import org.http4k.ai.a2a.model.AuthScheme
import org.http4k.ai.a2a.model.Task
import org.http4k.ai.a2a.model.TaskPushNotificationConfig
import org.http4k.ai.a2a.protocol.messages.toWire
import org.http4k.ai.a2a.util.A2AJson.json
import org.http4k.client.JavaHttpClient
import org.http4k.core.HttpHandler
import org.http4k.core.Method.POST
import org.http4k.core.Request

fun interface PushNotificationSender {
    operator fun invoke(task: Task, config: TaskPushNotificationConfig)

    companion object {
        fun Http(http: HttpHandler = JavaHttpClient()) =
            PushNotificationSender { task, config ->
                http(
                    Request(POST, config.pushNotificationConfig.url)
                        .json(task.toWire())
                        .withAuth(config)
                )
            }

        val NoOp = PushNotificationSender { _, _ -> }
    }
}

private fun Request.withAuth(config: TaskPushNotificationConfig): Request {
    val pushConfig = config.pushNotificationConfig
    val token = pushConfig.token ?: return this
    val auth = pushConfig.authentication ?: return this

    return when (auth.scheme) {
        AuthScheme.BEARER -> header("Authorization", "Bearer $token")
        AuthScheme.API_KEY -> header("X-API-Key", token)
        else -> this
    }
}
