package addressbook.user

import addressbook.shared.GetAllUsers
import addressbook.shared.GetAnAddress
import addressbook.shared.GetMyAddress
import addressbook.shared.UserDirectory
import addressbook.shared.UserId
import addressbook.user.UserPluginSettings.EMAIL
import addressbook.user.UserPluginSettings.PLUGIN_BASE_URL
import org.http4k.config.Environment
import org.http4k.config.Environment.Companion.ENV
import org.http4k.connect.openai.auth.user.UserLevelAuth
import org.http4k.connect.openai.info
import org.http4k.connect.openai.openAiPlugin
import org.http4k.core.RequestContexts
import org.http4k.core.then
import org.http4k.filter.ServerFilters.InitialiseRequestContext
import org.http4k.lens.RequestContextKey
import org.http4k.routing.RoutingHttpHandler

/**
 * Because we have the principal, this plugin has an extra endpoint for looking up the
 * authed user's address
 */
fun UserPlugin(env: Environment = ENV): RoutingHttpHandler {
    val userDirectory = UserDirectory()
    val contexts = RequestContexts()
    val userPrincipal = RequestContextKey.required<UserId>(contexts)

    return InitialiseRequestContext(contexts)
        .then(
            openAiPlugin(
                info(
                    apiVersion = "1.0",
                    humanDescription = "userplugin" to "A plugin which uses user-level auth",
                    pluginUrl = PLUGIN_BASE_URL(env),
                    contactEmail = EMAIL(env),
                ),
                UserLevelAuth(userDirectory.authUser(userPrincipal)),
                GetMyAddress(userDirectory, userPrincipal),
                GetAnAddress(userDirectory),
                GetAllUsers(userDirectory)
            )
        )
}

