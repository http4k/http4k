package addressbook.env

import addressbook.noauth.NoAuthPlugin
import addressbook.noauth.NoAuthPluginSettings.EMAIL
import addressbook.noauth.NoAuthPluginSettings.PLUGIN_BASE_URL
import addressbook.noauth.NoAuthPluginSettings.PORT
import org.http4k.config.Environment.Companion.ENV
import org.http4k.connect.openai.auth.OpenAIPluginId
import org.http4k.connect.openai.model.Email
import org.http4k.connect.openai.plugins.NoAuthPluginIntegration
import org.http4k.connect.openai.plugins.PluginIntegration
import org.http4k.core.Uri
import org.http4k.core.then
import org.http4k.core.with
import org.http4k.filter.CorsPolicy.Companion.UnsafeGlobalPermissive
import org.http4k.filter.ServerFilters.Cors
import org.http4k.server.SunHttp
import org.http4k.server.asServer

fun startNoAuthPlugin(): PluginIntegration {
    val env = ENV.with(
        PORT of 10000,
        PLUGIN_BASE_URL of Uri.of("http://localhost:10000"),
        EMAIL of Email.of("foo@bar.com"),
    )

    Cors(UnsafeGlobalPermissive)
        .then(NoAuthPlugin(env))
        .asServer(SunHttp(PORT(env)))
        .start()

    return NoAuthPluginIntegration(
        OpenAIPluginId.of("noauth"),
        PLUGIN_BASE_URL(env)
    )
}
