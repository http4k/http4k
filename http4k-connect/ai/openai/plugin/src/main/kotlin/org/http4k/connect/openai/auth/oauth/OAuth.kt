package org.http4k.connect.openai.auth.oauth

import org.http4k.connect.openai.auth.PluginAuth
import org.http4k.connect.openai.auth.oauth.internal.PluginAccessTokens
import org.http4k.connect.openai.auth.oauth.internal.PluginAuthorizationCodes
import org.http4k.connect.openai.auth.oauth.internal.PluginSecurityFilter
import org.http4k.connect.openai.auth.oauth.internal.StaticOpenAiClientValidator
import org.http4k.connect.openai.model.AuthedSystem
import org.http4k.connect.openai.model.VerificationToken
import org.http4k.core.Method.GET
import org.http4k.core.Method.POST
import org.http4k.core.then
import org.http4k.routing.bind
import org.http4k.security.oauth.server.AuthRequestTracking
import org.http4k.security.oauth.server.AuthorizationCodes
import org.http4k.security.oauth.server.OAuthServer
import java.time.Clock

/**
 * OAuth plugin auth. Uses an AuthorizationCode grant to auth the user to OpenAI. Uses some concepts from
 * http4k-security-oauth and some new ones to help with creation of a plugin.
 */
class OAuth<Principal : Any>(
    config: OAuthPluginConfig,
    principalTokens: PrincipalTokens<Principal>,
    principalChallenge: PrincipalChallenge<Principal>,
    principalStore: PrincipalStore<Principal>,
    authorizationCodes: AuthorizationCodes,
    authRequestTracking: AuthRequestTracking,
    tokens: Map<AuthedSystem, VerificationToken>,
    clock: Clock,
) : PluginAuth {

    override val manifestDescription = mapOf(
        "type" to "oauth",
        "client_url" to config.providerConfig.authPath,
        "scope" to config.scope,
        "authorization_url" to config.providerConfig.tokenPath,
        "authorization_content_type" to config.contentType,
        "verification_tokens" to tokens
    )

    private val server = OAuthServer(
        config.providerConfig.tokenPath,
        authRequestTracking,
        StaticOpenAiClientValidator(config),
        PluginAuthorizationCodes(authorizationCodes, principalStore, principalChallenge),
        PluginAccessTokens(principalStore, principalTokens),
        clock,
        refreshTokens = principalTokens
    )

    override val securityFilter = PluginSecurityFilter(principalTokens)

    override val authRoutes = listOf(
        server.tokenRoute,
        config.providerConfig.authPath bind GET to server.authenticationStart.then(principalChallenge.challenge),
        config.providerConfig.authPath bind POST to server.authenticationStart
            .then(principalChallenge.handleChallenge)
            .then(server.authenticationComplete)
    )
}
