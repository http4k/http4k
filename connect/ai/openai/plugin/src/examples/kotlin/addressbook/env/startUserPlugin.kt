package addressbook.env

import addressbook.shared.UserDirectory
import addressbook.user.UserPlugin
import addressbook.user.UserPluginSettings.EMAIL
import addressbook.user.UserPluginSettings.PLUGIN_BASE_URL
import addressbook.user.UserPluginSettings.PORT
import org.http4k.config.Environment.Companion.ENV
import org.http4k.connect.openai.auth.OpenAIPluginId
import org.http4k.connect.openai.model.Email
import org.http4k.connect.openai.plugins.PluginIntegration
import org.http4k.connect.openai.plugins.UserPluginIntegration
import org.http4k.core.Uri
import org.http4k.core.then
import org.http4k.core.with
import org.http4k.filter.CorsPolicy.Companion.UnsafeGlobalPermissive
import org.http4k.filter.ServerFilters.BasicAuth
import org.http4k.filter.ServerFilters.Cors
import org.http4k.filter.debug
import org.http4k.server.SunHttp
import org.http4k.server.asServer

fun startUserPlugin(): PluginIntegration {
    val env = ENV.with(
        PORT of 40000,
        PLUGIN_BASE_URL of Uri.of("http://localhost:40000"),
        EMAIL of Email.of("foo@bar.com"),
    )

    Cors(UnsafeGlobalPermissive)
        .then(UserPlugin(env))
        .debug()
        .asServer(SunHttp(PORT(env)))
        .start()

    return UserPluginIntegration(
        BasicAuth("") { UserDirectory().auth(it) != null },
        OpenAIPluginId.of("userplugin"),
        PLUGIN_BASE_URL(env)
    )
}
