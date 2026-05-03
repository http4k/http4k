/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.ai.a2a.server

import org.http4k.ai.a2a.model.StreamItem
import org.http4k.ai.a2a.model.Task
import org.http4k.ai.a2a.model.TaskId
import org.http4k.ai.a2a.util.A2AJson
import org.http4k.sse.Sse
import org.http4k.sse.SseMessage
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArraySet

interface TaskSubscriptions {
    fun subscribe(taskId: TaskId, sse: Sse)
    fun notify(task: Task)

    companion object {
        fun InMemory(): TaskSubscriptions = object : TaskSubscriptions {
            private val subscribers = ConcurrentHashMap<TaskId, CopyOnWriteArraySet<Sse>>()

            override fun subscribe(taskId: TaskId, sse: Sse) {
                val set = subscribers.getOrPut(taskId) { CopyOnWriteArraySet() }
                set.add(sse)
                sse.onClose { set.remove(sse) }
            }

            override fun notify(task: Task) {
                subscribers[task.id]?.forEach { sse ->
                    try {
                        sse.send(SseMessage.Data(A2AJson.asJsonString(task as StreamItem, StreamItem::class)))
                    } catch (_: Exception) {
                        subscribers[task.id]?.remove(sse)
                    }
                }
            }
        }

        fun NoOp(): TaskSubscriptions = object : TaskSubscriptions {
            override fun subscribe(taskId: TaskId, sse: Sse) {}
            override fun notify(task: Task) {}
        }
    }
}
