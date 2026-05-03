/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
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
            version = Version.of("1.0.0"),
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
