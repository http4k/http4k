import org.http4k.connect.storage.InMemory
import org.http4k.connect.storage.Storage
import org.http4k.connect.storage.asHttpHandler
import org.http4k.server.SunHttp
import org.http4k.server.asServer

fun main() {
    Storage.InMemory<Any>().asHttpHandler().asServer(SunHttp(8000)).start()
}
