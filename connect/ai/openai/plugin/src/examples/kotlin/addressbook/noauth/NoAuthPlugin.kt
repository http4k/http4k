package addressbook.noauth

import addressbook.noauth.NoAuthPluginSettings.EMAIL
import addressbook.noauth.NoAuthPluginSettings.PLUGIN_BASE_URL
import addressbook.shared.GetAllUsers
import addressbook.shared.GetAnAddress
import addressbook.shared.UserDirectory
import org.http4k.config.Environment
import org.http4k.config.Environment.Companion.ENV
import org.http4k.connect.openai.auth.noauth.NoAuth
import org.http4k.connect.openai.info
import org.http4k.connect.openai.openAiPlugin
import org.http4k.routing.RoutingHttpHandler

/**
 * A service-level plugin operates by authing the calling service only.
 */
fun NoAuthPlugin(env: Environment = ENV): RoutingHttpHandler {
    val userDirectory = UserDirectory()
    return openAiPlugin(
        info(
            apiVersion = "1.0",
            humanDescription = "noauthplugin" to "A plugin which uses no auth",
            pluginUrl = PLUGIN_BASE_URL(env),
            contactEmail = EMAIL(env),
        ),
        NoAuth,
        GetAnAddress(userDirectory),
        GetAllUsers(userDirectory)
    )
}
