package org.http4k.security.oauth.server

import com.natpryce.get
import com.natpryce.map
import com.natpryce.mapFailure
import org.http4k.core.HttpHandler
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status.Companion.OK
import org.http4k.core.with
import org.http4k.security.AccessTokenResponse
import org.http4k.security.accessTokenResponseBody
import org.http4k.security.oauth.server.accesstoken.GenerateAccessTokenForGrantType
import org.http4k.security.oauth.server.accesstoken.GrantTypesConfiguration
import java.time.Clock

class GenerateAccessToken(
        authorizationCodes: AuthorizationCodes,
        accessTokens: AccessTokens,
        clock: Clock,
        idTokens: IdTokens,
        private val errorRenderer: JsonResponseErrorRenderer,
        grantTypes: GrantTypesConfiguration
) : HttpHandler {

    private val generator = GenerateAccessTokenForGrantType(authorizationCodes, accessTokens, clock, idTokens, grantTypes)

    override fun invoke(request: Request) = generator.generate(request)
        .map { token ->
                    Response(OK).with(accessTokenResponseBody of AccessTokenResponse(
                            token.accessToken.value,
                            token.accessToken.type,
                            token.accessToken.expiresIn,
                            token.idToken?.value,
                            token.accessToken.scope))
        }.mapFailure(errorRenderer::response).get()
}
