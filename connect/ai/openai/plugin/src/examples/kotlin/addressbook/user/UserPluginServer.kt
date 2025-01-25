package addressbook.user

import addressbook.user.UserPluginSettings.PORT
import org.http4k.config.Environment
import org.http4k.config.Environment.Companion.ENV
import org.http4k.server.SunHttp
import org.http4k.server.asServer

/**
 * Binds the Plugin to a server and starts it as a JVM app
 */
fun UserPluginServer(env: Environment = ENV) = UserPlugin(env).asServer(SunHttp(PORT(env)))

fun main() {
    UserPluginServer().start()
}
