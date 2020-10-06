package cookbook.customised_servers

import org.eclipse.jetty.server.HttpConfiguration
import org.eclipse.jetty.server.HttpConnectionFactory
import org.eclipse.jetty.server.SecureRequestCustomizer
import org.eclipse.jetty.server.Server
import org.eclipse.jetty.server.ServerConnector
import org.eclipse.jetty.server.SslConnectionFactory
import org.eclipse.jetty.util.ssl.SslContextFactory
import org.http4k.core.HttpHandler
import org.http4k.core.Response
import org.http4k.core.Status
import org.http4k.core.then
import org.http4k.filter.DebuggingFilters.PrintRequestAndResponse
import org.http4k.server.Http4kServer
import org.http4k.server.ServerConfig
import org.http4k.server.asServer
import org.http4k.server.toJettyHandler

class SecureJetty(
    private val sslPort: Int,
    private val localKeyStorePath: String,
    private val localKeystorePassword: String,
    private val locakKeyManagerPassword: String

) : ServerConfig {
    override fun toServer(httpHandler: HttpHandler): Http4kServer {
        val server = Server().apply {
            val https = HttpConfiguration().apply {
                addCustomizer(SecureRequestCustomizer())
            }

            val sslContextFactory = SslContextFactory.Server().apply {
                this.keyStorePath = localKeyStorePath
                setKeyStorePassword(localKeystorePassword)
                setKeyManagerPassword(locakKeyManagerPassword)
            }

            connectors = arrayOf(
                ServerConnector(
                    server,
                    SslConnectionFactory(sslContextFactory, "http/1.1"),
                    HttpConnectionFactory(https)
                ).apply { port = sslPort }
            )

            insertHandler(httpHandler.toJettyHandler())
        }

        return object : Http4kServer {
            override fun start(): Http4kServer = apply { server.start() }

            override fun stop(): Http4kServer = apply { server.stop() }

            override fun port(): Int = if (sslPort > 0) sslPort else server.uri.port
        }
    }
}

fun main() {
    PrintRequestAndResponse().then { Response(Status.OK).body("hello from secure jetty!") }
        .asServer(SecureJetty(9000, "keystore.jks", "password", "password")).start()
}
