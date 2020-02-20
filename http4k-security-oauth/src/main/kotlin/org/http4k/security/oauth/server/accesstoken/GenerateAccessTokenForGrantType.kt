package org.http4k.security.oauth.server.accesstoken

import com.natpryce.Failure
import com.natpryce.Result
import com.natpryce.Success
import com.natpryce.flatMap
import com.natpryce.onFailure
import org.http4k.core.Body
import org.http4k.core.Request
import org.http4k.lens.FormField
import org.http4k.lens.Validator
import org.http4k.lens.webForm
import org.http4k.security.AccessTokenDetails
import org.http4k.security.oauth.server.AccessTokenError
import org.http4k.security.oauth.server.AccessTokens
import org.http4k.security.oauth.server.AuthorizationCodes
import org.http4k.security.oauth.server.IdTokens
import org.http4k.security.oauth.server.UnsupportedGrantType
import org.http4k.security.oauth.server.accesstoken.GrantType.*
import org.http4k.security.oauth.server.refreshToken.RefreshTokenAccessTokenGenerator
import org.http4k.security.oauth.server.refreshToken.RefreshTokens
import org.http4k.security.oauth.server.tokenRequest
import java.time.Clock

class GenerateAccessTokenForGrantType(
    authorizationCodes: AuthorizationCodes,
    accessTokens: AccessTokens,
    clock: Clock,
    idTokens: IdTokens,
    refreshTokens: RefreshTokens,
    private val grantTypes: GrantTypesConfiguration
) {
    private val authorizationCode = AuthorizationCodeAccessTokenGenerator(authorizationCodes, accessTokens, clock, idTokens)
    private val clientCredentials = ClientCredentialsAccessTokenGenerator(accessTokens)
    private val refreshTokens = RefreshTokenAccessTokenGenerator(refreshTokens)

    fun generate(request: Request): Result<AccessTokenDetails, AccessTokenError> {
        val grantType = resolveGrantTypeFromRequest(request).onFailure { return it }
        return resolveGrantTypeFromConfiguration(grantType)
            .flatMap { (generator, authenticator) ->
                authenticator.validateCredentials(request, request.tokenRequest(grantType))
                        .flatMap { (request, clientId, tokenRequest) ->
                            generator.generate(request, clientId, tokenRequest)
                        }
            }
    }

    private fun resolveGrantTypeFromConfiguration(grantType: GrantType) =
        grantTypes.supportedGrantTypes[grantType]?.let { Success(grantType.generator() to it) }
            ?: Failure(UnsupportedGrantType(grantType.rfcValue))

    private fun resolveGrantTypeFromRequest(request: Request): Result<GrantType, UnsupportedGrantType> {
        grantTypeForm(request).let { form ->
            return when (val grantType = grantType(form)) {
                AuthorizationCode.rfcValue -> Success(AuthorizationCode)
                ClientCredentials.rfcValue -> Success(ClientCredentials)
                RefreshToken.rfcValue -> Success(RefreshToken)
                else -> Failure(UnsupportedGrantType(grantType))
            }
        }
    }

    private fun GrantType.generator() = when (this) {
        AuthorizationCode -> authorizationCode
        ClientCredentials -> clientCredentials
        RefreshToken -> refreshTokens
    }

    companion object {
        val grantType = FormField.required("grant_type")
        val grantTypeForm = Body.webForm(Validator.Strict, grantType).toLens()
    }
}
