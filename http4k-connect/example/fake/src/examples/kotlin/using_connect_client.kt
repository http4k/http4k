import dev.forkhandles.result4k.Result
import org.http4k.client.JavaHttpClient
import org.http4k.connect.RemoteFailure
import org.http4k.connect.example.Example
import org.http4k.connect.example.FakeExample
import org.http4k.connect.example.Http
import org.http4k.connect.example.action.Echoed
import org.http4k.connect.example.echo
import org.http4k.core.HttpHandler
import org.http4k.filter.debug

const val USE_REAL_CLIENT = false

fun main() {
    // we can connect to the real service or the fake (drop in replacement)
    val http: HttpHandler = if (USE_REAL_CLIENT) JavaHttpClient() else FakeExample()

    // create a client
    val example = Example.Http(http.debug())

    // all operations return a Result monad of the API type
    val echoedResult: Result<Echoed, RemoteFailure> = example.echo("hello")
    println(echoedResult)
}
