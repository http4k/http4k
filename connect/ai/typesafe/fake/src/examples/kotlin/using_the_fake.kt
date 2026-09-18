import org.http4k.ai.model.ApiKey
import org.http4k.chaos.start
import org.http4k.client.JavaHttpClient
import org.http4k.connect.successValue
import org.http4k.connect.typesafe.Answer
import org.http4k.connect.typesafe.Confidence
import org.http4k.connect.typesafe.FakeTypeSafe
import org.http4k.connect.typesafe.Http
import org.http4k.connect.typesafe.Probability
import org.http4k.connect.typesafe.Question
import org.http4k.connect.typesafe.QuestionId
import org.http4k.connect.typesafe.TypeSafe
import org.http4k.connect.typesafe.ask
import org.http4k.core.Uri
import org.http4k.core.then
import org.http4k.filter.ClientFilters.SetBaseUriFrom

val alwaysTechnical = FakeTypeSafe { _, id, _ ->
    when (id) {
        QuestionId.of("department") -> Answer.Choice(
            "technical",
            mapOf("billing" to Probability.of(0.05), "technical" to Probability.of(0.95)),
            Confidence.of(0.95)
        )

        else -> throw IllegalArgumentException("No scripted answer for $id")
    }
}

fun main() {
    val port = alwaysTechnical.start().port()

    val typeSafe = TypeSafe.Http(
        ApiKey.of("ignored-by-the-fake"),
        SetBaseUriFrom(Uri.of("http://localhost:$port")).then(JavaHttpClient())
    )

    val routing = Question.Choice(
        "department", "Which team should handle this",
        mapOf("billing" to "Payment issues", "technical" to "Bugs or integration problems")
    )

    val answer = typeSafe.ask("my integration is broken", routing).successValue()

    println(answer.choice + " at " + answer.confidence)
}
