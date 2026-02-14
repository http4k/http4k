package org.http4k.ai.a2a.server.notification

import org.http4k.ai.a2a.model.AuthScheme
import org.http4k.ai.a2a.model.Task
import org.http4k.ai.a2a.model.TaskPushNotificationConfig
import org.http4k.ai.a2a.util.A2AJson.auto
import org.http4k.client.JavaHttpClient
import org.http4k.core.Body
import org.http4k.core.HttpHandler
import org.http4k.core.Method.POST
import org.http4k.core.Request
import org.http4k.core.with

fun interface PushNotificationSender {
    operator fun invoke(task: Task, config: TaskPushNotificationConfig)

    companion object {
        fun Http(http: HttpHandler = JavaHttpClient()) =
            PushNotificationSender { task, config ->
                http(
                    Request(POST, config.pushNotificationConfig.url)
                        .with(Body.auto<Task>().toLens() of task)
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
    val scheme = auth.schemes.firstOrNull() ?: return this

    return when (scheme) {
        AuthScheme.BEARER -> header("Authorization", "Bearer $token")
        AuthScheme.API_KEY -> header("X-API-Key", token)
        else -> this
    }
}
