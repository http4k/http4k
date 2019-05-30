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
import java.time.Clock

class GenerateAccessToken(
    clientValidator: ClientValidator,
    authorizationCodes: AuthorizationCodes,
    accessTokens: AccessTokens,
    clock: Clock,
    idTokens: IdTokens,
    private val errorRenderer: ErrorRenderer
) : HttpHandler {

    private val generator = GenerateAccessTokenForGrantType(clientValidator, authorizationCodes, accessTokens, clock, idTokens)

    override fun invoke(request: Request): Response {
        return generator.generate(request)
            .map { token ->
                Response(OK).let {
                    when {
                        token.idToken == null -> it.body(token.accessToken.value)
                        else -> it.with(accessTokenResponseBody of AccessTokenResponse(token.accessToken.value, token.idToken.value))
                    }
                }
            }.mapFailure(errorRenderer::response).get()
    }
}