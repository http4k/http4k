package addressbook.env

import addressbook.oauth.OAuthPlugin
import addressbook.oauth.OAuthPluginSettings.COOKIE_DOMAIN
import addressbook.oauth.OAuthPluginSettings.EMAIL
import addressbook.oauth.OAuthPluginSettings.OPENAI_CLIENT_CREDENTIALS
import addressbook.oauth.OAuthPluginSettings.OPENAI_PLUGIN_ID
import addressbook.oauth.OAuthPluginSettings.OPENAI_VERIFICATION_TOKEN
import addressbook.oauth.OAuthPluginSettings.PLUGIN_BASE_URL
import addressbook.oauth.OAuthPluginSettings.PORT
import addressbook.oauth.OAuthPluginSettings.REDIRECTION_URLS
import org.http4k.config.Environment.Companion.ENV
import org.http4k.connect.openai.auth.OpenAIPluginId
import org.http4k.connect.openai.auth.oauth.openaiPlugin
import org.http4k.connect.openai.model.Email
import org.http4k.connect.openai.model.VerificationToken
import org.http4k.connect.openai.plugins.OAuthPluginIntegration
import org.http4k.connect.openai.plugins.PluginIntegration
import org.http4k.core.Credentials
import org.http4k.core.Uri
import org.http4k.core.then
import org.http4k.core.with
import org.http4k.filter.CorsPolicy.Companion.UnsafeGlobalPermissive
import org.http4k.filter.ServerFilters.Cors
import org.http4k.security.OAuthProvider
import org.http4k.server.SunHttp
import org.http4k.server.asServer

fun startOAuthPlugin(openAiPort: Int): PluginIntegration {
    val pluginId = OpenAIPluginId.of("oauthplugin")

    val env = ENV.with(
        PORT of 20000,
        OPENAI_PLUGIN_ID of pluginId,
        PLUGIN_BASE_URL of Uri.of("http://localhost:20000"),
        OPENAI_CLIENT_CREDENTIALS of Credentials("foo", "bar"),
        EMAIL of Email.of("foo@bar.com"),
        COOKIE_DOMAIN of "localhost",
        OPENAI_VERIFICATION_TOKEN of VerificationToken.of("supersecret"),
        REDIRECTION_URLS of listOf(
            Uri.of("http://localhost:$openAiPort/aip/plugin-${pluginId}/oauth/callback")
        )
    )

    Cors(UnsafeGlobalPermissive)
        .then(OAuthPlugin(env))
        .asServer(SunHttp(PORT(env)))
        .start()

    return OAuthPluginIntegration(
        OPENAI_PLUGIN_ID(env),
        OAuthProvider.openaiPlugin(
            PLUGIN_BASE_URL(env),
            OPENAI_CLIENT_CREDENTIALS(env)
        ),
    )
}
