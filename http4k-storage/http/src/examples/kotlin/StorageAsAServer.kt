import org.http4k.server.SunHttp
import org.http4k.server.asServer
import org.http4k.storage.InMemory
import org.http4k.storage.Storage
import org.http4k.storage.asHttpHandler

fun main() {
    Storage.InMemory<Any>().asHttpHandler().asServer(SunHttp(8000)).start()
}
