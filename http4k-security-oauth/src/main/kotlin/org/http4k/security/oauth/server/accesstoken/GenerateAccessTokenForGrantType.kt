package org.http4k.security.oauth.server.accesstoken

import com.natpryce.Failure
import com.natpryce.Result
import com.natpryce.Success
import com.natpryce.flatMap
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
import org.http4k.security.oauth.server.InvalidClientCredentials
import org.http4k.security.oauth.server.UnsupportedGrantType
import org.http4k.security.oauth.server.accesstoken.GrantType.AuthorizationCode
import org.http4k.security.oauth.server.accesstoken.GrantType.ClientCredentials
import java.time.Clock

class GenerateAccessTokenForGrantType(
    authorizationCodes: AuthorizationCodes,
    accessTokens: AccessTokens,
    clock: Clock,
    idTokens: IdTokens,
    private val grantTypes: GrantTypesConfiguration
) {
    private val authorizationCode = AuthorizationCodeAccessTokenGenerator(authorizationCodes, accessTokens, clock, idTokens)
    private val clientCredentials = ClientCredentialsAccessTokenGenerator(accessTokens)

    fun generate(request: Request): Result<AccessTokenDetails, AccessTokenError> {
        return resolveGrantTypeFromRequest(request)
            .flatMap { resolveGrantTypeFromConfiguration(it) }
            .flatMap { (generator, authenticator) ->
                if (!authenticator.validateCredentials(request))
                    Failure(InvalidClientCredentials)
                else generator.generate(request)
            }
    }

    private fun resolveGrantTypeFromConfiguration(grantType: GrantType) =
        grantTypes.supportedGrantTypes[grantType]?.let { Success(grantType.generator() to it) }
            ?: Failure(UnsupportedGrantType(grantType.rfcValue))

    private fun resolveGrantTypeFromRequest(request: Request): Result<GrantType, UnsupportedGrantType> {
        grantTypeForm(request).let { form ->
            val grantType = grantType(form)
            return when (grantType) {
                AuthorizationCode.rfcValue -> Success(AuthorizationCode)
                ClientCredentials.rfcValue -> Success(ClientCredentials)
                else -> Failure(UnsupportedGrantType(grantType))
            }
        }
    }

    private fun GrantType.generator() = when (this) {
        AuthorizationCode -> authorizationCode
        ClientCredentials -> clientCredentials
    }

    companion object {
        val grantType = FormField.required("grant_type")
        val grantTypeForm = Body.webForm(Validator.Strict, grantType).toLens()
    }
}
