import dev.forkhandles.result4k.onFailure
import org.http4k.chaos.start
import org.http4k.client.JavaHttpClient
import org.http4k.connect.model.ModelName
import org.http4k.connect.openai.FakeOpenAI
import org.http4k.connect.openai.GPT3_5
import org.http4k.connect.openai.Http
import org.http4k.connect.openai.OpenAI
import org.http4k.connect.openai.OpenAIToken
import org.http4k.connect.model.Role.Companion.User
import org.http4k.connect.openai.action.Message
import org.http4k.connect.openai.action.Size
import org.http4k.connect.openai.chatCompletion
import org.http4k.connect.openai.generateImage
import org.http4k.core.Uri
import org.http4k.core.then
import org.http4k.filter.ClientFilters.SetBaseUriFrom

fun main() {
    val openAiToken = OpenAIToken.of("your-token-here")

    // start the fake on the default port
    val port = FakeOpenAI().start().port()

    // create the OpenAI instance pointing to our fake
    val openai = OpenAI.Http(
        openAiToken, SetBaseUriFrom(Uri.of("http://localhost:$port"))
            .then(JavaHttpClient())
    )

    // get a chat completion
    openai
        .chatCompletion(ModelName.GPT3_5, listOf(Message.User("good afternoon")), 1000, true)
        .onFailure { error(it) }
        .toList()
        .first()
        .choices
        ?.forEach {
            println(it.message?.role)
            println(it.message?.content)
        }

    // generate an image
    openai
        .generateImage("an amazing view", Size.`1024x1024`)
        .onFailure { error(it) }
        .data
        .forEach {
            println("See what I generated at: ${it.url}")
        }
}
