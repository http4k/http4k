/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.ai.a2a.server.storage

import org.http4k.ai.a2a.model.Task
import org.http4k.ai.a2a.model.Tenant
import org.http4k.ai.a2a.server.TaskSubscriptions

fun TaskStorage.withSubscriptions(subscriptions: TaskSubscriptions): TaskStorage =
    SubscribingTaskStorage(this, subscriptions)

private class SubscribingTaskStorage(
    private val delegate: TaskStorage,
    private val subscriptions: TaskSubscriptions
) : TaskStorage by delegate {

    override fun store(task: Task, tenant: Tenant?) {
        delegate.store(task, tenant)
        subscriptions.notify(task)
    }
}
