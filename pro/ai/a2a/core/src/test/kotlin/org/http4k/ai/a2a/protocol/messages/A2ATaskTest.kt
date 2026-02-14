package org.http4k.ai.a2a.protocol.messages

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.http4k.ai.a2a.model.ContextId
import org.http4k.ai.a2a.model.TaskId
import org.http4k.ai.a2a.model.TaskState
import org.http4k.ai.a2a.util.A2AJson
import org.http4k.core.ContentType.Companion.APPLICATION_JSON
import org.http4k.testing.Approver
import org.http4k.testing.JsonApprovalTest
import org.http4k.testing.assertApproved
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(JsonApprovalTest::class)
class A2ATaskTest {

    @Test
    fun `Get Request roundtrips correctly`(approver: Approver) {
        val request = A2ATask.Get.Request(
            id = TaskId.of("task-123"),
            historyLength = 10
        )
        val json = A2AJson.asFormatString(request)
        approver.assertApproved(json, APPLICATION_JSON)
        assertThat(A2AJson.asA<A2ATask.Get.Request>(json), equalTo(request))
    }

    @Test
    fun `Cancel Request roundtrips correctly`(approver: Approver) {
        val request = A2ATask.Cancel.Request(id = TaskId.of("task-456"))
        val json = A2AJson.asFormatString(request)
        approver.assertApproved(json, APPLICATION_JSON)
        assertThat(A2AJson.asA<A2ATask.Cancel.Request>(json), equalTo(request))
    }

    @Test
    fun `Resubscribe Request roundtrips correctly`(approver: Approver) {
        val request = A2ATask.Resubscribe.Request(id = TaskId.of("task-789"))
        val json = A2AJson.asFormatString(request)
        approver.assertApproved(json, APPLICATION_JSON)
        assertThat(A2AJson.asA<A2ATask.Resubscribe.Request>(json), equalTo(request))
    }

    @Test
    fun `Get method is tasks_get`() {
        assertThat(A2ATask.Get.Method.value, equalTo("tasks/get"))
    }

    @Test
    fun `Cancel method is tasks_cancel`() {
        assertThat(A2ATask.Cancel.Method.value, equalTo("tasks/cancel"))
    }

    @Test
    fun `Resubscribe method is tasks_resubscribe`() {
        assertThat(A2ATask.Resubscribe.Method.value, equalTo("tasks/resubscribe"))
    }

    @Test
    fun `List method is tasks_list`() {
        assertThat(A2ATask.List.Method.value, equalTo("tasks/list"))
    }

    @Test
    fun `List Request roundtrips correctly`(approver: Approver) {
        val request = A2ATask.List.Request(
            contextId = ContextId.of("ctx-123"),
            status = TaskState.working,
            pageSize = 25,
            pageToken = "token-abc"
        )
        val json = A2AJson.asFormatString(request)
        approver.assertApproved(json, APPLICATION_JSON)
        assertThat(A2AJson.asA<A2ATask.List.Request>(json), equalTo(request))
    }

    @Test
    fun `List Response roundtrips correctly`(approver: Approver) {
        val response = A2ATask.List.Response(
            tasks = emptyList(),
            nextPageToken = "next-token",
            pageSize = 50,
            totalSize = 100
        )
        val json = A2AJson.asFormatString(response)
        approver.assertApproved(json, APPLICATION_JSON)
        assertThat(A2AJson.asA<A2ATask.List.Response>(json), equalTo(response))
    }
}
