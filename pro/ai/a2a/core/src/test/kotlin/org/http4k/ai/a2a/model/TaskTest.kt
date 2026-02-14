package org.http4k.ai.a2a.model

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
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
        val task = Task(
            id = TaskId.of("task-123"),
            contextId = ContextId.of("context-456"),
            status = TaskStatus(state = TaskState.submitted)
        )
        val json = A2AJson.asFormatString(task)
        approver.assertApproved(json, APPLICATION_JSON)
        assertThat(A2AJson.asA<Task>(json), equalTo(task))
    }
}
