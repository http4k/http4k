package org.http4k.connect.amazon.cognito

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.http4k.connect.amazon.cognito.action.RespondToAuthChallenge
import org.http4k.connect.amazon.cognito.model.ChallengeName
import org.http4k.connect.amazon.cognito.model.ClientId
import org.http4k.connect.amazon.cognito.model.Session
import org.junit.jupiter.api.Test

class ActualResponseToAuthChallengeRequestsTest {
    val factory = CognitoMoshi

    @Test
    fun `serialize with EMAIL_OTP`() {
        val actual = RespondToAuthChallenge(
            ClientId = ClientId.of("1example23456789"),
            ChallengeName = ChallengeName.EMAIL_OTP,
            ChallengeResponses = mapOf(
                "USERNAME" to "testuser",
                "EMAIL_OTP_CODE" to "12345678"
            ),
            Session = Session.of("AYABeC1-y8qooiuysEv0uM4wAqQAHQABAAdTZXJ2aWNlABBDb2duaXRvVXNlclBvb2xzAAEAB2F3cy1rbXMAS2Fybjphd3M6a21zOnVzLXd...")
        )
        val expected = """
            {
              "ClientId": "1example23456789",
              "ChallengeName": "EMAIL_OTP",
              "ChallengeResponses": {
                "USERNAME" : "testuser",
                "EMAIL_OTP_CODE": "12345678"
              },
              "Session": "AYABeC1-y8qooiuysEv0uM4wAqQAHQABAAdTZXJ2aWNlABBDb2duaXRvVXNlclBvb2xzAAEAB2F3cy1rbXMAS2Fybjphd3M6a21zOnVzLXd..."
            }
        """.trimIndent()

        assertSerializedJson(actual, expected)
    }

    private fun <T : Any> assertSerializedJson(actual: T, expected: String) {
        assertThat(
            factory.prettify(factory.asFormatString(actual)),
            equalTo(factory.prettify((expected)))
        )
    }
}
