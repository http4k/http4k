package org.http4k.security.oauth.server

import org.http4k.core.Body
import org.http4k.core.Response
import org.http4k.core.Status
import org.http4k.core.with
import org.http4k.security.AccessTokenDetails
import org.http4k.security.AccessTokenResponse
import org.http4k.security.oauth.server.OAuthServerMoshi.auto
import org.http4k.security.openid.IdToken

fun interface AccessTokenResponseRenderer : (AccessTokenDetails) -> Response

object DefaultAccessTokenResponseRenderer : AccessTokenResponseRenderer {
    private val lens = Body.auto<AccessTokenResponse>()
        .map(
            { AccessTokenDetails(it.toAccessToken(), it.id_token?.let(::IdToken)) },
            {
                AccessTokenResponse(
                    it.accessToken.value,
                    it.accessToken.type,
                    it.accessToken.expiresIn,
                    it.idToken?.value,
                    it.accessToken.scope,
                    it.accessToken.refreshToken?.value
                )
            }
        ).toLens()

    override fun invoke(p1: AccessTokenDetails) = Response(Status.OK).with(lens of p1)
}
