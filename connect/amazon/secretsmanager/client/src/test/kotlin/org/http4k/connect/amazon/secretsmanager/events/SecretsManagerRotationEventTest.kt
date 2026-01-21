package org.http4k.connect.amazon.secretsmanager.events

import com.amazonaws.services.lambda.runtime.Context
import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import dev.forkhandles.mock4k.mock
import org.http4k.connect.amazon.secretsmanager.SecretsManagerMoshi
import org.http4k.connect.amazon.secretsmanager.model.SecretId
import org.http4k.core.ContentType
import org.http4k.serverless.FnHandler
import org.http4k.serverless.FnLoader
import org.http4k.testing.Approver
import org.http4k.testing.JsonApprovalTest
import org.http4k.testing.assertApproved
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.util.UUID

@ExtendWith(JsonApprovalTest::class)
class SecretsManagerRotationEventTest {

    private val sample = SecretsManagerRotationEvent(
        ClientRequestToken = UUID.fromString("30687bc4-18da-46b1-b4fe-7dcdfbb2baa8"),
        RotationToken = UUID.fromString("0f531e0e-781f-40f0-b23c-c79db8115409"),
        SecretId = SecretId.parse("arn:aws:secretsmanager:us-east-1:1234567890:secret:mySecret"),
        Step = Step.createSecret
    )

    @Test
    fun `can roundtrip event`(approval: Approver) {
        val json = SecretsManagerMoshi.asFormatString(sample)
        approval.assertApproved(json, ContentType.APPLICATION_JSON)

        assertThat(
            SecretsManagerMoshi.asA<SecretsManagerRotationEvent>(json),
            equalTo(sample)
        )
    }

    @Test
    fun `can launch a custom function`() {
        val fnLoader = FnLoader(SecretsManagerMoshi) {
            FnHandler { e: SecretsManagerRotationEvent, _: Context ->
                assertThat(e.SecretId, equalTo(sample.SecretId))
                assertThat(e.Step, equalTo(sample.Step))
                assertThat(e.RotationToken, equalTo(sample.RotationToken))
                assertThat(e.ClientRequestToken, equalTo(sample.ClientRequestToken))
                e.SecretId
            }
        }

        val result = fnLoader(emptyMap())(SecretsManagerMoshi.asInputStream(sample), mock())

        assertThat(result.reader().readText(), equalTo(sample.SecretId.value))
    }
}
