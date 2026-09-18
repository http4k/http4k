package org.http4k.connect.typesafe

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import com.natpryce.hamkrest.isWithin
import com.natpryce.hamkrest.present
import org.http4k.connect.successValue
import org.http4k.connect.typesafe.action.GetModels
import org.http4k.connect.typesafe.action.SystemOne
import org.junit.jupiter.api.Test

interface TypeSafeContract {

    val typeSafe: TypeSafe

    val department
        get() = Question.Choice(
            "department", "Which team should handle this",
            mapOf(
                "billing" to "Payment or subscription issues",
                "technical" to "Bugs or integration problems",
                "sales" to "Pricing or account questions"
            )
        )

    val frustration
        get() = Question.Score(
            "frustration", "How frustrated the customer appears",
            listOf("Calm, just stating facts", "Frustrated but civil", "Very angry, strong language")
        )

    val isUrgent get() = Question.Noul("is_urgent", "The message conveys urgency or time-sensitivity")

    val complaint
        get() = "I've been trying to connect my Stripe account for 3 days and it keeps failing. I'm losing sales."

    @Test
    fun `answers every question it is asked, at the right type`() {
        val response = typeSafe(SystemOne(complaint, questions = arrayOf(department, frustration, isUrgent)))
            .successValue()

        val choice = response.answerTo(department)
        assertThat(department.criteria.keys.contains(choice.choice), equalTo(true))
        assertThat(choice.probabilities.keys, equalTo(department.criteria.keys))

        val score = response.answerTo(frustration)
        assertThat(score.score, isWithin(0.0..(frustration.criteria.size - 1).toDouble()))
        assertThat(score.legend.keys, equalTo(frustration.criteria.indices.map(Int::toString).toSet()))

        assertThat(response.answerTo(isUrgent).noul.value, isWithin(0.0..1.0))
    }

    @Test
    fun `choice probabilities are a distribution over every option`() {
        val choice = typeSafe.ask(complaint, department).successValue()

        assertThat(choice.probabilities.values.sumOf { it.value }, isWithin(0.98..1.02))
        assertThat(choice.probabilities[choice.choice], present())
    }

    @Test
    fun `a question that was not asked has no answer`() {
        val response = typeSafe(SystemOne(complaint, questions = arrayOf(isUrgent))).successValue()

        assertThat(response[department], com.natpryce.hamkrest.absent())
    }

    @Test
    fun `lists the models this account can use`() {
        val models = typeSafe(GetModels).successValue().models

        assertThat(models.isNotEmpty(), equalTo(true))
        assertThat(models.map { it.name.value }.contains("jev-latest"), equalTo(true))
    }
}
