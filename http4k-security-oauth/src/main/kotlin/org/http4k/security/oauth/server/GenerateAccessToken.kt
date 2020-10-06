package org.http4k.security.oauth.server

import dev.forkhandles.result4k.get
import dev.forkhandles.result4k.map
import dev.forkhandles.result4k.mapFailure
import org.http4k.core.HttpHandler
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status.Companion.OK
import org.http4k.core.with
import org.http4k.security.AccessTokenResponse
import org.http4k.security.accessTokenResponseBody
import org.http4k.security.oauth.server.accesstoken.GenerateAccessTokenForGrantType
import org.http4k.security.oauth.server.accesstoken.GrantTypesConfiguration
import org.http4k.security.oauth.server.refreshtoken.RefreshTokens
import java.time.Clock

class GenerateAccessToken(
    authorizationCodes: AuthorizationCodes,
    accessTokens: AccessTokens,
    clock: Clock,
    idTokens: IdTokens,
    refreshTokens: RefreshTokens,
    private val errorRenderer: JsonResponseErrorRenderer,
    grantTypes: GrantTypesConfiguration
) : HttpHandler {

    private val generator = GenerateAccessTokenForGrantType(authorizationCodes, accessTokens, clock, idTokens, refreshTokens, grantTypes)

    override fun invoke(request: Request) = generator.generate(request)
        .map { token ->
            Response(OK).with(
                accessTokenResponseBody of AccessTokenResponse(
                    token.accessToken.value,
                    token.accessToken.type,
                    token.accessToken.expiresIn,
                    token.idToken?.value,
                    token.accessToken.scope,
                    token.accessToken.refreshToken?.value
                )
            )
        }.mapFailure(errorRenderer::response).get()
}
