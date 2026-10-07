package org.http4k.ai.llm.decide

import com.natpryce.hamkrest.allOf
import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import com.natpryce.hamkrest.has
import com.natpryce.hamkrest.isA
import com.natpryce.hamkrest.isIn
import com.natpryce.hamkrest.isWithin
import dev.forkhandles.result4k.orThrow
import org.http4k.ai.model.ModelName
import org.junit.jupiter.api.Test

interface DecideContract {

    val decide: Decide
    val model: ModelName

    @Test
    fun `answers every question it is asked, at the right type`() {
        val department = Question.Choice(
            "Which team should handle this",
            mapOf(
                "billing" to "Payment or subscription issues",
                "technical" to "Bugs or integration problems",
                "sales" to "Pricing or account questions"
            )
        )
        val frustration = Question.Score(
            "How frustrated the customer appears",
            listOf(
                Question.Score.Level("Calm", "Just stating facts"),
                Question.Score.Level("Frustrated but civil"),
                Question.Score.Level("Very angry", "Strong language")
            )
        )
        val isUrgent = Question.YesNo("The message conveys urgency or time-sensitivity")

        val response = decide(
            DecideRequest(
                "I've been trying to connect my Stripe account for 3 days and it keeps failing. I'm losing sales.",
                mapOf("department" to department, "frustration" to frustration, "is_urgent" to isUrgent),
                model
            )
        ).orThrow { error(it) }

        assertThat(response.answers.keys, equalTo(setOf("department", "frustration", "is_urgent")))
        assertThat(
            response.answers.getValue("department"), isA<Answer.Choice>(
                allOf(
                    has(Answer.Choice::choice, isIn(department.options.keys)),
                    has(Answer.Choice::probabilities, has(Map<String, Double>::keys, equalTo(department.options.keys)))
                )
            )
        )
        assertThat(
            response.answers.getValue("frustration"),
            isA<Answer.Score>(has(Answer.Score::score, isWithin(0.0..(frustration.levels.size - 1).toDouble())))
        )
        assertThat(
            response.answers.getValue("is_urgent"),
            isA<Answer.YesNo>(has(Answer.YesNo::probability, isWithin(0.0..1.0)))
        )
    }
}
