package addressbook.oauth

import addressbook.oauth.OAuthPluginSettings.COOKIE_DOMAIN
import addressbook.oauth.OAuthPluginSettings.EMAIL
import addressbook.oauth.OAuthPluginSettings.OPENAI_CLIENT_CREDENTIALS
import addressbook.oauth.OAuthPluginSettings.OPENAI_PLUGIN_ID
import addressbook.oauth.OAuthPluginSettings.OPENAI_VERIFICATION_TOKEN
import addressbook.oauth.OAuthPluginSettings.PLUGIN_BASE_URL
import addressbook.oauth.OAuthPluginSettings.REDIRECTION_URLS
import addressbook.oauth.auth.InMemoryStorageProvider
import addressbook.oauth.auth.LoginPrincipalChallenge
import addressbook.oauth.auth.SimplePrincipalAccessTokens
import addressbook.oauth.auth.StorageAuthRequestTracking
import addressbook.oauth.auth.StorageAuthorizationCodes
import addressbook.oauth.auth.StoragePrincipalStore
import addressbook.shared.GetAllUsers
import addressbook.shared.GetAnAddress
import addressbook.shared.GetMyAddress
import addressbook.shared.UserDirectory
import org.http4k.config.Environment
import org.http4k.config.Environment.Companion.ENV
import org.http4k.connect.openai.auth.oauth.OAuth
import org.http4k.connect.openai.auth.oauth.OAuthPluginConfig
import org.http4k.connect.openai.auth.oauth.openaiPlugin
import org.http4k.connect.openai.info
import org.http4k.connect.openai.model.AuthedSystem.Companion.openai
import org.http4k.connect.openai.openAiPlugin
import org.http4k.routing.RoutingHttpHandler
import org.http4k.security.OAuthProvider
import java.time.Clock
import java.time.Clock.systemUTC
import java.time.Duration.ofMinutes

/**
 * A plugin which is protected by an OAuth AuthorizationCode flow (using a custom login screen)
 */
fun OAuthPlugin(
    env: Environment = ENV,
    clock: Clock = systemUTC()
): RoutingHttpHandler {
    val userDirectory = UserDirectory()
    val storageProvider = InMemoryStorageProvider()

    val accessTokens = SimplePrincipalAccessTokens()
    return openAiPlugin(
        info(
            apiVersion = "1.0",
            humanDescription = "oauthplugin" to "A plugin which uses oauth",
            pluginUrl = PLUGIN_BASE_URL(env),
            contactEmail = EMAIL(env),
        ),
        OAuth(
            OAuthPluginConfig(
                OPENAI_PLUGIN_ID(env),
                OAuthProvider.openaiPlugin(PLUGIN_BASE_URL(env), OPENAI_CLIENT_CREDENTIALS(env)),
                REDIRECTION_URLS(env)
            ),
            accessTokens,
            LoginPrincipalChallenge(userDirectory),
            StoragePrincipalStore(storageProvider()),
            StorageAuthorizationCodes(storageProvider(), clock, ofMinutes(1)),
            StorageAuthRequestTracking(storageProvider(), COOKIE_DOMAIN(env), clock, ofMinutes(1)),
            mapOf(openai to OPENAI_VERIFICATION_TOKEN(env)),
            clock
        ),
        GetMyAddress(userDirectory, accessTokens::resolve),
        GetAnAddress(userDirectory),
        GetAllUsers(userDirectory)
    )
}

