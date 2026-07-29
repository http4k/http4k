/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.ai.mcp.stateless.model

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.http4k.ai.mcp.stateless.model.TaskStatus.cancelled
import org.http4k.ai.mcp.stateless.model.TaskStatus.completed
import org.http4k.ai.mcp.stateless.model.TaskStatus.failed
import org.http4k.ai.mcp.stateless.model.TaskStatus.input_required
import org.http4k.ai.mcp.stateless.model.TaskStatus.working
import org.junit.jupiter.api.Test
import java.util.Random

class TaskTest {

    @Test
    fun `task ids are unguessable`() {
        // the spec permits a taskId to be used as a bearer token, so ids must not be enumerable
        val ids = (1..100).map { TaskId.random() }

        assertThat(ids.toSet().size, equalTo(100))
    }

    @Test
    fun `task id generation is driven by the supplied randomness`() {
        val fixed = { Random(0) }

        assertThat(TaskId.random(fixed()), equalTo(TaskId.random(fixed())))
    }

    @Test
    fun `a client stops polling on the terminal statuses`() {
        assertThat(TaskStatus.entries.filter { it.isTerminal }, equalTo(listOf(completed, cancelled, failed)))
        assertThat(TaskStatus.entries.filterNot { it.isTerminal }, equalTo(listOf(working, input_required)))
    }
}
