/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.ai.a2a.server.protocol

import org.http4k.ai.a2a.model.Task
import org.http4k.ai.a2a.protocol.messages.A2ATask

interface Tasks {
    fun get(params: A2ATask.Get.Request.Params): A2ATask.Get.Response.Result?
    fun store(task: Task)
    fun cancel(params: A2ATask.Cancel.Request.Params): A2ATask.Cancel.Response.Result?
    fun list(params: A2ATask.List.Request.Params): A2ATask.List.Response.Result
}
