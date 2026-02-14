package org.http4k.ai.a2a.model

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.http4k.ai.a2a.util.A2AJson
import org.http4k.ai.model.Role
import org.http4k.core.ContentType.Companion.APPLICATION_JSON
import org.http4k.testing.Approver
import org.http4k.testing.JsonApprovalTest
import org.http4k.testing.assertApproved
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(JsonApprovalTest::class)
class MessageTest {

    @Test
    fun `Message roundtrips correctly`(approver: Approver) {
        val message = Message(
            role = Role.User,
            parts = listOf(Part.Text("Hello")),
            messageId = MessageId.of("msg-123")
        )
        val json = A2AJson.asFormatString(message)
        approver.assertApproved(json, APPLICATION_JSON)
        assertThat(A2AJson.asA<Message>(json), equalTo(message))
    }
}
