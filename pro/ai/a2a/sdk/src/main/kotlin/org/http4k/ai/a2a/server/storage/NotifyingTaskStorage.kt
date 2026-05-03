/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.ai.a2a.server.storage

import org.http4k.ai.a2a.model.Task
import org.http4k.ai.a2a.model.Tenant
import org.http4k.ai.a2a.server.notification.PushNotificationSender

fun TaskStorage.withPushNotifications(
    configStorage: PushNotificationConfigStorage,
    sender: PushNotificationSender
): TaskStorage = NotifyingTaskStorage(this, configStorage, sender)

private class NotifyingTaskStorage(
    private val delegate: TaskStorage,
    private val configStorage: PushNotificationConfigStorage,
    private val sender: PushNotificationSender
) : TaskStorage by delegate {

    override fun store(task: Task, tenant: Tenant?) {
        delegate.store(task, tenant)
        configStorage.list(task.id, tenant = tenant).configs.forEach { sender(task, it) }
    }
}
