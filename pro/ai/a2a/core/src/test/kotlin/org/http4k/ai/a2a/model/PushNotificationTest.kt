package org.http4k.ai.a2a.model

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.http4k.ai.a2a.model.AuthScheme.Companion.BEARER
import org.http4k.ai.a2a.util.A2AJson
import org.http4k.core.ContentType.Companion.APPLICATION_JSON
import org.http4k.core.Uri
import org.http4k.testing.Approver
import org.http4k.testing.JsonApprovalTest
import org.http4k.testing.assertApproved
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(JsonApprovalTest::class)
class PushNotificationTest {

    @Test
    fun `PushNotificationConfig roundtrips correctly`(approver: Approver) {
        val config = PushNotificationConfig(
            url = Uri.of("https://example.com/webhook"),
            token = "secret-token",
            authentication = AgentAuthentication(schemes = listOf(BEARER))
        )
        val json = A2AJson.asFormatString(config)
        approver.assertApproved(json, APPLICATION_JSON)
        assertThat(A2AJson.asA<PushNotificationConfig>(json), equalTo(config))
    }

    @Test
    fun `PushNotificationConfig without optional fields roundtrips correctly`(approver: Approver) {
        val config = PushNotificationConfig(
            url = Uri.of("https://example.com/webhook")
        )
        val json = A2AJson.asFormatString(config)
        approver.assertApproved(json, APPLICATION_JSON)
        assertThat(A2AJson.asA<PushNotificationConfig>(json), equalTo(config))
    }

    @Test
    fun `PushNotificationConfigId is a value type`() {
        val id = PushNotificationConfigId.of("config-123")
        assertThat(id.value, equalTo("config-123"))
    }

    @Test
    fun `AgentAuthentication roundtrips correctly`(approver: Approver) {
        val auth = AgentAuthentication(schemes = listOf(BEARER, AuthScheme.OAUTH2))
        val json = A2AJson.asFormatString(auth)
        approver.assertApproved(json, APPLICATION_JSON)
        assertThat(A2AJson.asA<AgentAuthentication>(json), equalTo(auth))
    }
}
