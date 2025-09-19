package org.http4k.connect.amazon.cognito.action

import org.http4k.connect.Http4kConnectAction
import org.http4k.connect.amazon.cognito.CognitoAction
import org.http4k.connect.amazon.cognito.model.AuthenticationResult
import org.http4k.connect.amazon.cognito.model.ClientId
import org.http4k.connect.amazon.cognito.model.ClientSecret
import org.http4k.connect.amazon.cognito.model.ContextData
import org.http4k.connect.amazon.cognito.model.RefreshToken
import se.ansman.kotshi.JsonSerializable

@Http4kConnectAction
@JsonSerializable
    data class GetTokensFromRefreshToken(
    val ClientId: ClientId,
    val ClientMetadata: Map<String, String>? = null,
    val ClientSecret: ClientSecret? = null,
    val ContextData: ContextData? = null,
    val DeviceKey: String? = null,
    val RefreshToken: RefreshToken
) : CognitoAction<GetTokensFromRefreshTokenResponse>(GetTokensFromRefreshTokenResponse::class)

@JsonSerializable
data class GetTokensFromRefreshTokenResponse(
    val AuthenticationResult: AuthenticationResult
)
