package org.http4k.connect.amazon.cognito.action

import org.http4k.connect.Http4kConnectAction
import org.http4k.connect.amazon.cognito.CognitoAction
import org.http4k.connect.amazon.cognito.model.AnalyticsMetadata
import org.http4k.connect.amazon.cognito.model.AuthFlow
import org.http4k.connect.amazon.cognito.model.ClientId
import org.http4k.connect.amazon.cognito.model.ContextData
import org.http4k.connect.amazon.cognito.model.UserPoolId
import se.ansman.kotshi.JsonSerializable

@Http4kConnectAction
@JsonSerializable
data class AdminInitiateAuth(
    val ClientId: ClientId,
    val AuthFlow: AuthFlow,
    val UserPoolId: UserPoolId,
    val AuthParameters: Map<String, String>? = null,
    val ClientMetadata: Map<String, String>? = null,
    val ContextData: ContextData? = null,
    val AnalyticsMetadata: AnalyticsMetadata? = null
) : CognitoAction<AuthInitiated>(AuthInitiated::class)
