package addressbook.oauth.auth

import addressbook.shared.UserId
import dev.forkhandles.result4k.Success
import org.http4k.connect.openai.auth.oauth.PrincipalTokens
import org.http4k.connect.openai.auth.oauth.internal.accessToken
import org.http4k.core.Request
import org.http4k.security.AccessToken
import org.http4k.security.oauth.core.RefreshToken
import org.http4k.security.oauth.server.ClientId
import org.http4k.security.oauth.server.TokenRequest

/**
 * Creates OAuth tokens and resolves the principal.
 */
class SimplePrincipalAccessTokens : PrincipalTokens<UserId> {
    override fun resolve(accessToken: AccessToken): UserId = UserId.of(accessToken.value)

    override fun resolve(request: Request): UserId = request.accessToken()?.let(::resolve)!!

    override fun create(principal: UserId) = Success(
        AccessToken(principal.value, expiresIn = 1000, refreshToken = RefreshToken(principal.value.reversed()))
    )

    override fun refreshAccessToken(clientId: ClientId, tokenRequest: TokenRequest, refreshToken: RefreshToken) =
        create(UserId.of(refreshToken.value.reversed()))
}
