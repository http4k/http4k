package org.http4k.connect.amazon.cognito.endpoints

import org.http4k.connect.amazon.AwsJsonFake
import org.http4k.connect.amazon.cognito.CognitoPool
import org.http4k.connect.amazon.cognito.action.GetTokensFromRefreshToken
import org.http4k.connect.amazon.cognito.action.GetTokensFromRefreshTokenResponse
import org.http4k.connect.amazon.cognito.model.AuthenticationResult
import org.http4k.connect.amazon.cognito.model.AccessToken
import org.http4k.connect.amazon.cognito.model.IdToken
import org.http4k.connect.amazon.cognito.model.RefreshToken
import org.http4k.connect.storage.Storage

fun AwsJsonFake.getTokensFromRefreshToken(pools: Storage<CognitoPool>) = route<GetTokensFromRefreshToken> { request ->
    GetTokensFromRefreshTokenResponse(
        AuthenticationResult(
            AccessToken = AccessToken.of("fake-access-token-${System.currentTimeMillis()}"),
            ExpiresIn = 3600,
            IdToken = IdToken.of("fake-id-token-${System.currentTimeMillis()}"),
            RefreshToken = request.RefreshToken, // Return the same refresh token
            TokenType = "Bearer"
        )
    )
}
