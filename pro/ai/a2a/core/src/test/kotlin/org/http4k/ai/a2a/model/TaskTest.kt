/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.ai.a2a.model

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.http4k.ai.a2a.protocol.messages.A2ATask
import org.http4k.ai.a2a.protocol.messages.A2ATaskStatus
import org.http4k.ai.a2a.util.A2AJson
import org.http4k.core.ContentType.Companion.APPLICATION_JSON
import org.http4k.testing.Approver
import org.http4k.testing.JsonApprovalTest
import org.http4k.testing.assertApproved
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(JsonApprovalTest::class)
class TaskTest {

    @Test
    fun `Task roundtrips correctly`(approver: Approver) {
        val task = A2ATask(
            id = TaskId.of("task-123"),
            contextId = ContextId.of("context-456"),
            status = A2ATaskStatus(state = TaskState.TASK_STATE_SUBMITTED)
        )
        val json = A2AJson.asFormatString(task)
        approver.assertApproved(json, APPLICATION_JSON)
        assertThat(A2AJson.asA<A2ATask>(json), equalTo(task))
    }
}
