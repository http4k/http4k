package org.http4k.connect.openai

import org.http4k.connect.openai.action.DecisionAnswer
import org.http4k.connect.openai.action.DecisionAnswer.ChoiceProbability
import org.http4k.connect.openai.action.DecisionAnswer.ScoreProbability
import org.http4k.connect.openai.action.DecisionInputMessage
import org.http4k.connect.openai.action.DecisionQuestion
import kotlin.math.ln

fun interface DecisionAnswerer {
    operator fun invoke(input: List<DecisionInputMessage>, question: DecisionQuestion): DecisionAnswer
}

object FirstOptionAnswerer : DecisionAnswerer {
    override fun invoke(input: List<DecisionInputMessage>, question: DecisionQuestion) = when (question) {
        is DecisionQuestion.Predicate -> DecisionAnswer.Predicate(question.name, 0.5)
        is DecisionQuestion.Choice -> question.asAnswer()
        is DecisionQuestion.Score -> question.asAnswer()
    }

    private fun DecisionQuestion.Choice.asAnswer(): DecisionAnswer.Choice {
        val probabilities = choices.indices.favouringFirst()
        return DecisionAnswer.Choice(
            name,
            choices.first().value,
            choices.zip(probabilities) { choice, p -> ChoiceProbability(choice.value, p) },
            probabilities.asConfidence()
        )
    }

    private fun DecisionQuestion.Score.asAnswer(): DecisionAnswer.Score {
        val probabilities = levels.indices.favouringFirst()
        return DecisionAnswer.Score(
            name,
            probabilities.withIndex().sumOf { (index, p) -> index * p },
            levels.zip(probabilities).mapIndexed { index, (level, p) -> ScoreProbability(index, level.label, p) },
            probabilities.asConfidence()
        )
    }

    private fun IntRange.favouringFirst(): List<Double> {
        val total = count() + 1.0
        return map { if (it == first) 2 / total else 1 / total }
    }

    private fun List<Double>.asConfidence() = when {
        size <= 1 -> 1.0
        else -> 1.0 - filter { it > 0.0 }.sumOf { -it * ln(it) } / ln(size.toDouble())
    }
}
