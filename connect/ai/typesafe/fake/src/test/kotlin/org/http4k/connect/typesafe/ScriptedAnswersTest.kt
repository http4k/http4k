package org.http4k.connect.typesafe

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.http4k.connect.successValue
import org.junit.jupiter.api.Test

class ScriptedAnswersTest {

    private val department = Question.Choice(
        "department", "Which team should handle this",
        mapOf("billing" to "Payment issues", "technical" to "Bugs")
    )

    @Test
    fun `answers can be scripted per question`() {
        val typeSafe = FakeTypeSafe { _, id, _ ->
            when (id) {
                QuestionId.of("department") -> Answer.Choice(
                    "technical",
                    mapOf("billing" to Probability.of(0.1), "technical" to Probability.of(0.9)),
                    Confidence.of(0.9)
                )

                else -> throw IllegalArgumentException("unscripted question $id")
            }
        }.client()

        assertThat(typeSafe.ask("anything", department).successValue().choice, equalTo("technical"))
    }

    @Test
    fun `the default answerer is deterministic`() {
        val first = FakeTypeSafe().client().ask("a complaint", department).successValue()
        val second = FakeTypeSafe().client().ask("a different complaint", department).successValue()

        assertThat(first, equalTo(second))
    }
}
