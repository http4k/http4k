package org.http4k.ai.a2a.model

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.junit.jupiter.api.Test

class ValuesTest {

    @Test
    fun `TaskId can be created from string`() {
        val taskId = TaskId.of("task-123")
        assertThat(taskId.value, equalTo("task-123"))
    }

    @Test
    fun `ContextId can be created from string`() {
        val contextId = ContextId.of("context-456")
        assertThat(contextId.value, equalTo("context-456"))
    }

    @Test
    fun `MessageId can be created from string`() {
        val messageId = MessageId.of("msg-789")
        assertThat(messageId.value, equalTo("msg-789"))
    }

    @Test
    fun `ArtifactId can be created from string`() {
        val artifactId = ArtifactId.of("artifact-abc")
        assertThat(artifactId.value, equalTo("artifact-abc"))
    }
}
