package org.http4k.connect.amazon.cognito.action

import org.http4k.connect.Http4kConnectAction
import org.http4k.connect.amazon.cognito.CognitoAction
import org.http4k.connect.amazon.cognito.model.AnalyticsMetadata
import org.http4k.connect.amazon.cognito.model.ChallengeName
import org.http4k.connect.amazon.cognito.model.ClientId
import org.http4k.connect.amazon.cognito.model.ContextData
import org.http4k.connect.amazon.cognito.model.Session
import org.http4k.connect.amazon.cognito.model.UserPoolId
import se.ansman.kotshi.JsonSerializable

@Http4kConnectAction
@JsonSerializable
data class AdminRespondToAuthChallenge(
    val ChallengeName: ChallengeName,
    val ClientId: ClientId,
    val UserPoolId: UserPoolId,
    val Session: Session? = null,
    val ClientMetadata: Map<String, String>? = null,
    val ContextData: ContextData? = null,
    val ChallengeResponses: Map<String, String>? = null,
    val AnalyticsMetadata: AnalyticsMetadata? = null
) : CognitoAction<AuthInitiated>(AuthInitiated::class)
