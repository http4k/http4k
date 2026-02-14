package org.http4k.ai.a2a.model

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.http4k.ai.a2a.util.A2AJson
import org.http4k.core.ContentType.Companion.APPLICATION_JSON
import org.http4k.core.Uri
import org.http4k.testing.Approver
import org.http4k.testing.JsonApprovalTest
import org.http4k.testing.assertApproved
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(JsonApprovalTest::class)
class AgentCardTest {

    @Test
    fun `AgentCard roundtrips correctly`(approver: Approver) {
        val card = AgentCard(
            name = "Test Agent",
            description = "A test agent",
            url = Uri.of("https://example.com/agent"),
            version = "1.0.0",
            capabilities = AgentCapabilities(streaming = true),
            skills = listOf(
                AgentSkill(
                    id = SkillId.of("skill-1"),
                    name = "Test Skill",
                    description = "Does something"
                )
            )
        )
        val json = A2AJson.asFormatString(card)
        approver.assertApproved(json, APPLICATION_JSON)
        assertThat(A2AJson.asA<AgentCard>(json), equalTo(card))
    }
}
