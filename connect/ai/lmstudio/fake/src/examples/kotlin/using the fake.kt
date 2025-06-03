import dev.forkhandles.result4k.onFailure
import org.http4k.ai.model.MaxTokens
import org.http4k.ai.model.ModelName
import org.http4k.chaos.start
import org.http4k.client.JavaHttpClient
import org.http4k.connect.lmstudio.CHAT_MODEL
import org.http4k.connect.lmstudio.FakeLmStudio
import org.http4k.connect.lmstudio.Http
import org.http4k.connect.lmstudio.LmStudio
import org.http4k.connect.lmstudio.action.Message
import org.http4k.connect.lmstudio.chatCompletion
import org.http4k.core.Uri
import org.http4k.core.then
import org.http4k.filter.ClientFilters.SetBaseUriFrom

fun main() {
    // start the fake on the default port
    val port = FakeLmStudio().start().port()

    // create the OpenAI instance pointing to our fake
    val openai = LmStudio.Http(
        SetBaseUriFrom(Uri.of("http://localhost:$port"))
            .then(JavaHttpClient())
    )

    // get a chat completion
    openai
        .chatCompletion(ModelName.CHAT_MODEL, listOf(Message.User("good afternoon")), MaxTokens.of(100), true)
        .onFailure { error(it) }
        .toList()
        .first()
        .choices
        .forEach {
            println(it.message?.role)
            println(it.message?.content)
        }
}
