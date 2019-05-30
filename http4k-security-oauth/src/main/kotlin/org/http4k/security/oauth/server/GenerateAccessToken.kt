package org.http4k.security.oauth.server

import com.natpryce.flatMap
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
import java.time.Clock

class GenerateAccessToken(
    private val clientValidator: ClientValidator,
    private val authorizationCodes: AuthorizationCodes,
    private val accessTokens: AccessTokens,
    private val clock: Clock,
    private val idTokens: IdTokens,
    private val errorRenderer: ErrorRenderer
) : HttpHandler {

    override fun invoke(request: Request): Response {
        return request.accessTokenRequest()
            .flatMap { it.generate(clientValidator, authorizationCodes, accessTokens, clock, idTokens) }
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