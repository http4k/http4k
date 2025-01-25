package addressbook.service

import addressbook.service.ServicePluginSettings.EMAIL
import addressbook.service.ServicePluginSettings.OPENAI_API_KEY
import addressbook.service.ServicePluginSettings.OPENAI_VERIFICATION_TOKEN
import addressbook.service.ServicePluginSettings.PLUGIN_BASE_URL
import addressbook.shared.GetAllUsers
import addressbook.shared.GetAnAddress
import addressbook.shared.UserDirectory
import org.http4k.config.Environment
import org.http4k.config.Environment.Companion.ENV
import org.http4k.connect.openai.auth.PluginAuthToken.Bearer
import org.http4k.connect.openai.auth.service.ServiceLevelAuth
import org.http4k.connect.openai.info
import org.http4k.connect.openai.model.AuthedSystem.Companion.openai
import org.http4k.connect.openai.openAiPlugin
import org.http4k.routing.RoutingHttpHandler

/**
 * A service-level plugin operates by authing the calling service only.
 */
fun ServicePlugin(env: Environment = ENV): RoutingHttpHandler {
    val userDirectory = UserDirectory()
    return openAiPlugin(
        info(
            apiVersion = "1.0",
            humanDescription = "serviceplugin" to "A plugin which uses service-level auth",
            pluginUrl = PLUGIN_BASE_URL(env),
            contactEmail = EMAIL(env),
        ),
        ServiceLevelAuth(
            Bearer { it == OPENAI_API_KEY(env) },
            mapOf(openai to OPENAI_VERIFICATION_TOKEN(env))
        ),
        GetAnAddress(userDirectory),
        GetAllUsers(userDirectory)
    )
}
