import org.http4k.ai.model.ApiKey
import org.http4k.connect.successValue
import org.http4k.connect.typesafe.Http
import org.http4k.connect.typesafe.Question
import org.http4k.connect.typesafe.TypeSafe
import org.http4k.connect.typesafe.action.GetModels
import org.http4k.connect.typesafe.ask

fun main() {
    val typeSafe = TypeSafe.Http(ApiKey.of(System.getenv("TYPESAFE_API_KEY")))

    typeSafe(GetModels).successValue().models.forEach { println(it.name) }

    val refundRequested = Question.Noul("refund_requested", "Does the customer request a refund?")

    println(typeSafe.ask("I want my money back please", refundRequested).successValue().noul)

    val sentiment = Question.Score(
        "sentiment", "How positive is this message",
        listOf("Hostile", "Neutral", "Delighted")
    )

    println(typeSafe.ask("this is the best product I have ever used", sentiment).successValue().score)
}
