import org.http4k.ai.model.ApiKey
import org.http4k.ai.model.MaxTokens
import org.http4k.connect.openai.Http
import org.http4k.connect.openai.OpenAI
import org.http4k.connect.openai.OpenAIModels.GPT3_5
import org.http4k.connect.openai.action.Message
import org.http4k.connect.openai.chatCompletion

fun main() {
    val openAiToken = ApiKey.of(System.getenv("OPEN_AI_TOKEN"))

    // create a client
    val openai = OpenAI.Http(openAiToken)

    // get a chat completion
    println(
        openai.chatCompletion(
            GPT3_5,
            listOf(
                Message.User("Explain pythagoras's theorem to a 5 year old child")
            ),
            MaxTokens.of(1000),
            false
        )
    )
}
