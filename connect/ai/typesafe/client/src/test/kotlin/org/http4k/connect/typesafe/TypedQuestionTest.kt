package org.http4k.connect.typesafe

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.http4k.format.MoshiString
import org.junit.jupiter.api.Test

data class RubricLevel(val label: String, val meaning: String)

class TypedQuestionTest {

    private val levels = listOf(
        RubricLevel("calm", "polite enquiry"),
        RubricLevel("furious", "threats to cancel")
    )

    private val levelMaps = listOf(
        mapOf("label" to "calm", "meaning" to "polite enquiry"),
        mapOf("label" to "furious", "meaning" to "threats to cancel")
    )

    private val rubric = Entry<RubricLevel>()

    @Test
    fun `score criteria from data classes match the equivalent maps`() {
        assertThat(
            Question.Score("frustration", "How frustrated", levels),
            equalTo(Question.Score("frustration", "How frustrated", levelMaps))
        )
    }

    @Test
    fun `choice criteria from data classes match the equivalent maps`() {
        assertThat(
            Question.Choice("mood", "Pick the mood", mapOf("calm" to levels[0], "furious" to levels[1])),
            equalTo(Question.Choice("mood", "Pick the mood", mapOf("calm" to levelMaps[0], "furious" to levelMaps[1])))
        )
    }

    @Test
    fun `noul criteria from data classes match the equivalent maps`() {
        assertThat(
            Question.Noul("angry", "Is the customer angry", mapOf("true" to levels[1])),
            equalTo(Question.Noul("angry", "Is the customer angry", mapOf("true" to levelMaps[1])))
        )
    }

    @Test
    fun `criteria read back through the lens`() {
        assertThat(rubric(Question.Score("frustration", "How frustrated", levels).criteria), equalTo(levels))
        assertThat(
            rubric(Question.Choice("mood", "Pick the mood", mapOf("calm" to levels[0])).criteria),
            equalTo(mapOf("calm" to levels[0]))
        )
        assertThat(
            Question.Noul("angry", "Is the customer angry", mapOf("true" to levels[1])).criteria?.let { rubric(it) },
            equalTo(mapOf("true" to levels[1]))
        )
    }

    @Test
    fun `instructions read back as a node`() {
        assertThat(
            Question.Noul("angry", "Is the customer angry").instructions.asA<String>(),
            equalTo("Is the customer angry")
        )
    }

    @Test
    fun `legend built from values reads back through the lens`() {
        val answer = Answer.Score(0.5, levels, Confidence.of(0.9))

        assertThat(rubric(answer.legend), equalTo(mapOf("0" to levels[0], "1" to levels[1])))
    }

    @Test
    fun `the chosen criterion comes back at its declared type`() {
        val mood = Question.Choice("mood", "Pick the mood", mapOf("calm" to levels[0], "furious" to levels[1]))
        val answer = Answer.Choice("furious", mapOf("furious" to Probability.of(0.9)), Confidence.of(0.9))

        assertThat(answer.chosen(mood), equalTo(levels[1]))
    }

    @Test
    fun `pre-built node instructions pass through unchanged`() {
        assertThat(
            Question.Noul("angry", MoshiString("Is the customer angry")).instructions,
            equalTo(Question.Noul("angry", "Is the customer angry").instructions)
        )
    }
}
