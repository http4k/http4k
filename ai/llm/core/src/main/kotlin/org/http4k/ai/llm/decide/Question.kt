package org.http4k.ai.llm.decide

sealed interface Question {
    val instructions: String

    data class Choice(override val instructions: String, val options: Map<String, String>) : Question

    data class Score(override val instructions: String, val levels: List<Level>) : Question {
        data class Level(val label: String, val description: String? = null)
    }

    data class YesNo(override val instructions: String) : Question
}
