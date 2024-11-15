import dev.forkhandles.result4k.Result
import org.http4k.client.JavaHttpClient
import org.http4k.connect.RemoteFailure
import org.http4k.connect.ollama.FakeOllama
import org.http4k.connect.ollama.Http
import org.http4k.connect.ollama.Ollama
import org.http4k.connect.ollama.action.ModelList
import org.http4k.connect.ollama.getModels
import org.http4k.core.HttpHandler
import org.http4k.filter.debug

const val USE_REAL_CLIENT = false

fun main() {
    // we can connect to the real service or the fake (drop in replacement)
    val http: HttpHandler = if (USE_REAL_CLIENT) JavaHttpClient() else FakeOllama()

    // create a client
    val client = Ollama.Http(http.debug())

    // all operations return a Result monad of the API type
    val result: Result<ModelList, RemoteFailure> = client.getModels()

    println(result)
}
