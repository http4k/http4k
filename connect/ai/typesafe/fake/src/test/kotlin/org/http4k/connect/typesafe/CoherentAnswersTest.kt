package org.http4k.connect.typesafe

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import com.natpryce.hamkrest.isWithin
import org.http4k.connect.successValue
import org.junit.jupiter.api.Test

class CoherentAnswersTest {

    private val typeSafe = FakeTypeSafe().client()

    private val department = Question.Choice(
        "department", "Which team should handle this",
        mapOf("billing" to "Payment issues", "technical" to "Bugs", "sales" to "Pricing")
    )

    private val frustration = Question.Score(
        "frustration", "How frustrated the customer appears",
        listOf("Calm", "Frustrated but civil", "Very angry")
    )

    @Test
    fun `choice probabilities are a distribution over exactly the options offered`() {
        val choice = typeSafe.ask("a complaint", department).successValue()

        assertThat(choice.probabilities.keys, equalTo(department.criteria.keys))
        assertThat(choice.probabilities.values.sumOf { it.value }, isWithin(0.999..1.001))
    }

    @Test
    fun `the chosen option is the most probable one`() {
        val choice = typeSafe.ask("a complaint", department).successValue()

        assertThat(choice.probabilities.maxBy { it.value.value }.key, equalTo(choice.choice))
    }

    @Test
    fun `score is the probability-weighted mean of the level indices`() {
        val score = typeSafe.ask("a complaint", frustration).successValue()

        val weightedMean = score.probabilities!!.entries.sumOf { (level, p) -> level.toInt() * p.value }

        assertThat(score.score, isWithin((weightedMean - 0.001)..(weightedMean + 0.001)))
    }

    @Test
    fun `score legend echoes the criteria keyed by level index`() {
        val score = typeSafe.ask("a complaint", frustration).successValue()

        assertThat(score.legend.keys, equalTo(setOf("0", "1", "2")))
        assertThat(score.legend, equalTo(frustration.criteria.withIndex().associate { it.index.toString() to it.value }))
    }

    @Test
    fun `confidence falls as the distribution flattens`() {
        val twoOptions = typeSafe.ask("x", Question.Choice("a", "?", mapOf("1" to "a", "2" to "b"))).successValue()
        val manyOptions = typeSafe.ask(
            "x", Question.Choice("a", "?", (1..8).associate { it.toString() to "option $it" })
        ).successValue()

        assertThat(manyOptions.confidence.value < twoOptions.confidence.value, equalTo(true))
    }

    @Test
    fun `noul answers carry no confidence or probabilities to invent`() {
        val noul = typeSafe.ask("x", Question.Noul("urgent", "Is this urgent?")).successValue()

        assertThat(noul.noul, equalTo(Probability.of(0.5)))
    }
}
