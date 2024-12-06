import io.undertow.UndertowOptions.ENABLE_HTTP2
import org.http4k.core.HttpHandler
import org.http4k.server.Http4kServer
import org.http4k.server.PolyServerConfig
import org.http4k.server.ServerConfig.StopMode
import org.http4k.server.ServerConfig.StopMode.Immediate
import org.http4k.server.buildHttp4kUndertowServer
import org.http4k.server.buildUndertowHandlers
import org.http4k.server.defaultUndertowBuilder
import org.http4k.sse.SseHandler
import org.http4k.websocket.WsHandler

/**
 * Custom Undertow server configuration with http 2 support
 */
class CustomUndertow(
    val port: Int = 8000,
    val enableHttp2: Boolean = false,
    override val stopMode: StopMode = Immediate
) : PolyServerConfig {

    override fun toServer(http: HttpHandler?, ws: WsHandler?, sse: SseHandler?): Http4kServer {
        val (httpHandler, multiProtocolHandler) = buildUndertowHandlers(http, ws, sse, stopMode)

        return defaultUndertowBuilder(port, multiProtocolHandler)
            .setServerOption(ENABLE_HTTP2, enableHttp2)
            .buildHttp4kUndertowServer(
            httpHandler, stopMode, port
        )
    }
}
