package org.http4k.connect.typesafe

import org.http4k.format.MoshiNode
import org.http4k.format.unwrap
import kotlin.math.ln

fun interface QuestionAnswerer {
    operator fun invoke(state: MoshiNode, id: QuestionId, question: Question<*>): Answer
}

object FirstCriterionAnswerer : QuestionAnswerer {
    override fun invoke(state: MoshiNode, id: QuestionId, question: Question<*>) = when (question) {
        is Question.Choice -> question.asAnswer()
        is Question.Score -> question.asAnswer()
        is Question.Noul -> Answer.Noul(Probability.of(0.5))
    }

    private fun Question.Choice.asAnswer(): Answer.Choice {
        val probabilities = criteria.keys.favouring(criteria.keys.first())
        return Answer.Choice(
            criteria.keys.first(),
            probabilities.mapValues { Probability.of(it.value) },
            Confidence.of(probabilities.values.asConfidence())
        )
    }

    private fun Question.Score.asAnswer(): Answer.Score {
        val levels = criteria.indices.map(Int::toString)
        val probabilities = levels.favouring(levels.first())
        return Answer.Score(
            probabilities.entries.sumOf { (level, p) -> level.toInt() * p },
            criteria.withIndex().associate { (index, description) -> index.toString() to description },
            Confidence.of(probabilities.values.asConfidence()),
            probabilities.mapValues { Probability.of(it.value) }
        )
    }

    private fun Collection<String>.favouring(favourite: String): Map<String, Double> {
        val total = size + 1.0
        return associateWith { if (it == favourite) 2 / total else 1 / total }
    }

    private fun Collection<Double>.asConfidence() = when {
        size <= 1 -> 1.0
        else -> 1.0 - filter { it > 0.0 }.sumOf { -it * ln(it) } / ln(size.toDouble())
    }
}

internal fun MoshiNode.approximateTokens() = (unwrap()?.toString()?.length ?: 0) / 4
