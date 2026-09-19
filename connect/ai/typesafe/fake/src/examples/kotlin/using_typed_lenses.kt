import org.http4k.connect.successValue
import org.http4k.connect.typesafe.Answer
import org.http4k.connect.typesafe.Entry
import org.http4k.connect.typesafe.FakeTypeSafe
import org.http4k.connect.typesafe.FirstCriterionAnswerer
import org.http4k.connect.typesafe.Probability
import org.http4k.connect.typesafe.Question
import org.http4k.connect.typesafe.QuestionId
import org.http4k.connect.typesafe.asA
import org.http4k.connect.typesafe.chosen
import org.http4k.connect.typesafe.invoke
import org.http4k.connect.typesafe.systemOne

data class SupportCase(val message: String, val orderId: Int)

data class Severity(val label: String, val description: String)

val severityLevels = listOf(
    Severity("minor", "Cosmetic or a workaround exists"),
    Severity("major", "Blocks a key workflow"),
    Severity("critical", "The customer cannot operate")
)

val severity = Question.Score("severity", "How severe is the problem", severityLevels)

val impact = Question.Choice(
    "impact", "How badly does this affect the customer",
    severityLevels.associateBy { it.label }
)

val refundRisk = Question.Noul("refund_risk", "The customer is likely to demand a refund")

val scriptedByOrderValue = FakeTypeSafe { state, id, question ->
    val case: SupportCase = state.asA()
    when (id) {
        QuestionId.of("refund_risk") -> Answer.Noul(Probability.of(if (case.orderId > 1000) 0.8 else 0.2))
        else -> FirstCriterionAnswerer(state, id, question)
    }
}

fun main() {
    val typeSafe = scriptedByOrderValue.client()

    val response = typeSafe
        .systemOne(SupportCase("The dashboard crashes on login", 4242), severity, impact, refundRisk)
        .successValue()

    val severityLens = Entry<Severity>()

    println("levels asked about: " + severityLens(severity.criteria).map { it.label })
    println("severity score: " + response.answerTo(severity).score)
    println("scored against: " + severityLens(response.answerTo(severity).legend))
    println("impact: " + response.answerTo(impact).chosen<Severity>(impact).description)
    println("refund risk: " + response.answerTo(refundRisk).noul)
}
