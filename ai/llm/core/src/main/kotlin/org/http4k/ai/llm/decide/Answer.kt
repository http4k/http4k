package org.http4k.ai.llm.decide

sealed interface Answer {
    data class Choice(val choice: String, val probabilities: Map<String, Double>, val confidence: Double) : Answer

    data class Score(val score: Double, val probabilities: Map<Int, Double>, val confidence: Double) : Answer

    data class YesNo(val probability: Double) : Answer
}
