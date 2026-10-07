package org.http4k.ai.llm.decide

import dev.forkhandles.result4k.Failure
import dev.forkhandles.result4k.map
import dev.forkhandles.result4k.mapFailure
import org.http4k.ai.llm.LLMError.Http
import org.http4k.ai.llm.LLMError.Internal
import org.http4k.ai.llm.LLMResult
import org.http4k.ai.llm.model.Content
import org.http4k.ai.model.ApiKey
import org.http4k.client.JavaHttpClient
import org.http4k.connect.typesafe.Http
import org.http4k.connect.typesafe.TypeSafe
import org.http4k.connect.typesafe.action.SystemOne
import org.http4k.core.HttpHandler
import org.http4k.core.Response
import org.http4k.connect.typesafe.Answer as TypeSafeAnswer
import org.http4k.connect.typesafe.Question as TypeSafeQuestion

fun Decide.Companion.TypeSafe(apiKey: ApiKey, http: HttpHandler = JavaHttpClient()) = object : Decide {
    private val client = TypeSafe.Http(apiKey, http)

    override fun invoke(request: DecideRequest): LLMResult<DecideResponse> {
        val texts = request.state.filterIsInstance<Content.Text>()
        if (texts.size != request.state.size) {
            return Failure(Internal(IllegalArgumentException("TypeSafe only decides on text content")))
        }

        return client(
            SystemOne(
                texts.joinToString("\n") { it.text },
                *request.questions.map { (id, question) -> question.toTypeSafe(id) }.toTypedArray(),
                model = request.model
            )
        )
            .map {
                DecideResponse(
                    it.answers.entries.associate { (id, answer) -> id.value to answer.toLLM() },
                    DecideResponse.Metadata(it.model, it.tokenUsage)
                )
            }
            .mapFailure { Http(Response(it.status).body(it.message ?: "")) }
    }
}

private fun Question.toTypeSafe(id: String) = when (this) {
    is Question.Choice -> TypeSafeQuestion.Choice(id, instructions, options)
    is Question.Score -> TypeSafeQuestion.Score(id, instructions, levels.map { it.asCriterion() })
    is Question.YesNo -> TypeSafeQuestion.Noul(id, instructions)
}

private fun Question.Score.Level.asCriterion() = description?.let { "$label: $it" } ?: label

private fun TypeSafeAnswer.toLLM() = when (this) {
    is TypeSafeAnswer.Choice -> Answer.Choice(choice, probabilities.mapValues { it.value.value }, confidence.value)
    is TypeSafeAnswer.Score -> Answer.Score(
        score,
        probabilities.orEmpty().entries.associate { (level, p) -> level.toInt() to p.value },
        confidence.value
    )

    is TypeSafeAnswer.Noul -> Answer.YesNo(noul.value)
}
