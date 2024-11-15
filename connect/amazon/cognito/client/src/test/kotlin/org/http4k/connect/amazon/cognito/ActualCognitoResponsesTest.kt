package org.http4k.connect.amazon.cognito

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.http4k.connect.amazon.cognito.action.AuthInitiated
import org.http4k.connect.amazon.cognito.model.AccessToken
import org.http4k.connect.amazon.cognito.model.ChallengeName
import org.http4k.connect.amazon.cognito.model.IdToken
import org.http4k.connect.amazon.cognito.model.RefreshToken
import org.http4k.connect.amazon.cognito.model.Session
import org.junit.jupiter.api.Test

class ActualCognitoResponsesTest {

    val factory = CognitoMoshi

    @Test
    fun `deserialising a user password response with no challenges requested`() {
        val response = factory.asA<AuthInitiated>(
            """{"AuthenticationResult":
            {"AccessToken":"access-token",
            "ExpiresIn":3600,
            "IdToken":"id-token",
            "RefreshToken":"refresh-token",
            "TokenType":"Bearer"},
            "ChallengeParameters":{}}
        """.trimIndent()
        )
        assertThat(response.AuthenticationResult?.AccessToken, equalTo(AccessToken.of("access-token")))
        assertThat(response.AuthenticationResult?.ExpiresIn, equalTo(3600))
        assertThat(response.AuthenticationResult?.IdToken, equalTo(IdToken.of("id-token")))
        assertThat(response.AuthenticationResult?.RefreshToken, equalTo(RefreshToken.of("refresh-token")))
        assertThat(response.AuthenticationResult?.TokenType, equalTo("Bearer"))
        assertThat(response.ChallengeParameters, equalTo(mapOf()))
    }

    @Test
    fun `deserializing a user password response with a challenge`() {
        val response = factory.asA<AuthInitiated>(
            """
                {
                  "ChallengeName": "NEW_PASSWORD_REQUIRED",
                  "ChallengeParameters": {
                    "USER_ID_FOR_SRP": "user",
                    "requiredAttributes": "[]"
                  },
                  "Session": "session.id"
                }
            """.trimIndent()
        )

        assertThat(response.ChallengeName, equalTo(ChallengeName.NEW_PASSWORD_REQUIRED))
        assertThat(
            response.ChallengeParameters,
            equalTo(mapOf("USER_ID_FOR_SRP" to "user", "requiredAttributes" to "[]"))
        )
        assertThat(response.Session, equalTo(Session.of("session.id")))
    }
}
