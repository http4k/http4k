package org.http4k.connect.typesafe

import org.http4k.ai.model.ModelName
import org.http4k.ai.model.TokenUsage
import se.ansman.kotshi.JsonSerializable

@JsonSerializable
data class SystemOneResponse(
    val model: ModelName,
    val answers: Map<QuestionId, Answer>,
    val usage: Usage? = null
) {
    operator fun <A : Answer> get(question: Question<A>): A? = answers[question.id]?.let(question::answerFrom)

    fun <A : Answer> answerTo(question: Question<A>): A = get(question)
        ?: throw NoSuchElementException("No answer for question '${question.id.value}'")

    val tokenUsage get() = TokenUsage(usage?.input_tokens, usage?.output_tokens)
}

@JsonSerializable
data class Usage(val input_tokens: Int? = null, val output_tokens: Int? = null)
