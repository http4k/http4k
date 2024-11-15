package addressbook.service

import addressbook.service.ServicePluginSettings.PORT
import org.http4k.config.Environment
import org.http4k.config.Environment.Companion.ENV
import org.http4k.server.SunHttp
import org.http4k.server.asServer

/**
 * Binds the Plugin to a server and starts it as a JVM app
 */
fun ServicePluginServer(env: Environment = ENV) = ServicePlugin(env).asServer(SunHttp(PORT(env)))

fun main() {
    ServicePluginServer().start()
}
