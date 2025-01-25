package addressbook.env

import addressbook.service.ServicePlugin
import addressbook.service.ServicePluginSettings.OPENAI_API_KEY
import addressbook.service.ServicePluginSettings.OPENAI_VERIFICATION_TOKEN
import addressbook.user.UserPluginSettings.EMAIL
import addressbook.user.UserPluginSettings.PLUGIN_BASE_URL
import addressbook.user.UserPluginSettings.PORT
import org.http4k.config.Environment.Companion.ENV
import org.http4k.connect.openai.auth.OpenAIPluginId
import org.http4k.connect.openai.model.Email
import org.http4k.connect.openai.model.VerificationToken
import org.http4k.connect.openai.plugins.PluginIntegration
import org.http4k.connect.openai.plugins.ServicePluginIntegration
import org.http4k.core.Uri
import org.http4k.core.then
import org.http4k.core.with
import org.http4k.filter.ClientFilters.BearerAuth
import org.http4k.filter.CorsPolicy.Companion.UnsafeGlobalPermissive
import org.http4k.filter.ServerFilters
import org.http4k.security.AccessToken
import org.http4k.server.SunHttp
import org.http4k.server.asServer

fun startServicePlugin(): PluginIntegration {
    val env = ENV.with(
        PORT of 30000,
        PLUGIN_BASE_URL of Uri.of("http://localhost:30000"),
        OPENAI_API_KEY of AccessToken("foobar"),
        EMAIL of Email.of("foo@bar.com"),
        OPENAI_VERIFICATION_TOKEN of VerificationToken.of("barfoo")
    )

    ServerFilters.Cors(UnsafeGlobalPermissive)
        .then(ServicePlugin(env))
        .asServer(SunHttp(PORT(env)))
        .start()

    return ServicePluginIntegration(
        BearerAuth(OPENAI_API_KEY(env).value),
        OpenAIPluginId.of("serviceplugin"),
        PLUGIN_BASE_URL(env)
    )
}
