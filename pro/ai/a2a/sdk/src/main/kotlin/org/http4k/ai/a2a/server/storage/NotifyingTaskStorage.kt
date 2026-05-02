/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.ai.a2a.server.storage

import org.http4k.ai.a2a.model.ContextId
import org.http4k.ai.a2a.model.Task
import org.http4k.ai.a2a.model.TaskId
import org.http4k.ai.a2a.model.TaskState
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
) : TaskStorage {

    override fun store(task: Task, tenant: Tenant?) {
        delegate.store(task, tenant)
        configStorage.list(task.id, tenant).forEach { sender(task, it) }
    }

    override fun get(taskId: TaskId, tenant: Tenant?) = delegate.get(taskId, tenant)

    override fun delete(taskId: TaskId, tenant: Tenant?) = delegate.delete(taskId, tenant)

    override fun list(
        contextId: ContextId?,
        status: TaskState?,
        pageSize: Int?,
        pageToken: String?,
        tenant: Tenant?
    ) = delegate.list(contextId, status, pageSize, pageToken, tenant)
}
