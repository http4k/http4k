import org.reekwest.http.core.Request
import org.reekwest.http.core.Response
import org.reekwest.http.core.Status
import org.reekwest.http.core.toBody
import org.reekwest.http.netty.asNettyServer

fun main(args: Array<String>) {
    val server = { _: Request -> Response(Status.OK, body = "heeelllll".toBody()) }.asNettyServer(8000).start()
    println("my server is running!")
    server.stop()
    println("my server is no longer running!")
}