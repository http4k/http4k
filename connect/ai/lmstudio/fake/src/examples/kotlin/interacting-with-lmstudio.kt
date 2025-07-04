import org.http4k.ai.model.MaxTokens
import org.http4k.ai.model.ModelName
import org.http4k.connect.lmstudio.CHAT_MODEL
import org.http4k.connect.lmstudio.Http
import org.http4k.connect.lmstudio.LmStudio
import org.http4k.connect.lmstudio.action.Message.Companion.User
import org.http4k.connect.lmstudio.chatCompletion

fun main() {
    // create a client
    val lmstudio = LmStudio.Http()

    // get a chat completion
    println(
        lmstudio.chatCompletion(
            ModelName.CHAT_MODEL,
            listOf(
                User("Explain pythagoras's theorem to a 5 year old child"),
            ),
            MaxTokens.of(100),
            false
        )
    )
}
