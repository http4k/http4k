import dev.forkhandles.result4k.Result
import org.http4k.client.JavaHttpClient
import org.http4k.connect.RemoteFailure
import org.http4k.connect.azure.Http
import org.http4k.connect.azure.AzureAI
import org.http4k.connect.azure.AzureAIApiKey
import org.http4k.connect.azure.AzureHost
import org.http4k.connect.azure.FakeAzureAI
import org.http4k.connect.azure.Region
import org.http4k.connect.model.Role.Companion.User
import org.http4k.connect.azure.action.CompletionResponse
import org.http4k.connect.azure.action.Message
import org.http4k.connect.azure.chatCompletion
import org.http4k.connect.model.ModelName
import org.http4k.connect.orThrow
import org.http4k.core.HttpHandler
import org.http4k.filter.debug

const val USE_REAL_CLIENT = false

fun main() {
    // we can connect to the real service or the fake (drop in replacement)
    val http: HttpHandler = if (USE_REAL_CLIENT) JavaHttpClient() else FakeAzureAI()

    // create a client
    val client = AzureAI.Http(AzureAIApiKey.of("foobar"),
        AzureHost.of("foobar"), Region.of("foobar"),
        http.debug())

    // all operations return a Result monad of the API type
    val result: Result<Sequence<CompletionResponse>, RemoteFailure> = client
        .chatCompletion(ModelName.of("Meta-Llama-3.1-70B-Instruct"), listOf(Message.User("good afternoon")), 1000, true)

    println(result.orThrow().toList())
}
