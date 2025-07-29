package org.http4k.connect.amazon.cognito.action

import org.http4k.connect.Http4kConnectAction
import org.http4k.connect.amazon.cognito.CognitoAction
import org.http4k.connect.amazon.cognito.model.AnalyticsMetadata
import org.http4k.connect.amazon.cognito.model.ClientId
import org.http4k.connect.amazon.cognito.model.CodeDeliveryDetails
import org.http4k.connect.amazon.cognito.model.SecretHash
import org.http4k.connect.amazon.cognito.model.UserContextData
import org.http4k.connect.amazon.core.model.Username
import se.ansman.kotshi.JsonSerializable

@Http4kConnectAction
@JsonSerializable
data class ResendConfirmationCode(
    val AnalyticsMetaData: AnalyticsMetadata? = null,
    val ClientId: ClientId,
    val ClientMetadata: Map<String, String>? = null,
    val SecretHash: SecretHash? = null,
    val UserContextData: UserContextData? = null,
    val Username: Username
) : CognitoAction<ResendConfirmationCodeResponse>(ResendConfirmationCodeResponse::class)

@JsonSerializable
data class ResendConfirmationCodeResponse(
    val CodeDeliveryDetails: CodeDeliveryDetails
)
