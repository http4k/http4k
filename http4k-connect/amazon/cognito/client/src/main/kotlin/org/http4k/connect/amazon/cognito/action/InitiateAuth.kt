package org.http4k.connect.amazon.cognito.action

import org.http4k.connect.Http4kConnectAction
import org.http4k.connect.amazon.cognito.CognitoAction
import org.http4k.connect.amazon.cognito.model.AnalyticsMetadata
import org.http4k.connect.amazon.cognito.model.AuthFlow
import org.http4k.connect.amazon.cognito.model.AuthenticationResult
import org.http4k.connect.amazon.cognito.model.ChallengeName
import org.http4k.connect.amazon.cognito.model.ClientId
import org.http4k.connect.amazon.cognito.model.Session
import org.http4k.connect.amazon.cognito.model.UserContextData
import se.ansman.kotshi.JsonSerializable

@Http4kConnectAction
@JsonSerializable
data class InitiateAuth(
    val ClientId: ClientId,
    val AuthFlow: AuthFlow,
    val AuthParameters: Map<String, String>? = null,
    val ClientMetadata: Map<String, String>? = null,
    val UserContextData: UserContextData? = null,
    val AnalyticsMetadata: AnalyticsMetadata? = null
) : CognitoAction<AuthInitiated>(AuthInitiated::class)

@JsonSerializable
data class AuthInitiated(
    val AuthenticationResult: AuthenticationResult?,
    val ChallengeName: ChallengeName?,
    val ChallengeParameters: Map<String, String>?,
    val Session: Session?
)
