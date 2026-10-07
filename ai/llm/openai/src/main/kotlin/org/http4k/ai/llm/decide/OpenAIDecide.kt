package org.http4k.ai.llm.decide

import dev.forkhandles.result4k.Failure
import dev.forkhandles.result4k.map
import dev.forkhandles.result4k.mapFailure
import org.http4k.ai.llm.LLMError.Http
import org.http4k.ai.llm.LLMError.Internal
import org.http4k.ai.llm.LLMResult
import org.http4k.ai.llm.OpenAIApi
import org.http4k.ai.llm.OpenAICompatibleClient
import org.http4k.ai.llm.model.Content
import org.http4k.ai.llm.model.Resource
import org.http4k.ai.model.ApiKey
import org.http4k.ai.model.TokenUsage
import org.http4k.client.JavaHttpClient
import org.http4k.connect.openai.action.DecisionAnswer
import org.http4k.connect.openai.action.DecisionChoice
import org.http4k.connect.openai.action.DecisionInputMessage
import org.http4k.connect.openai.action.DecisionInputPart
import org.http4k.connect.openai.action.DecisionLevel
import org.http4k.connect.openai.action.DecisionQuestion
import org.http4k.connect.openai.action.ImageDetail
import org.http4k.connect.openai.createDecision
import org.http4k.core.HttpHandler
import org.http4k.core.Response

fun Decide.Companion.OpenAI(apiKey: ApiKey, http: HttpHandler = JavaHttpClient(), org: OpenAIApi.Org? = null) =
    OpenAI(OpenAIApi(apiKey, http, org))

fun Decide.Companion.OpenAI(openAICompatibleClient: OpenAICompatibleClient) = object : Decide {
    private val client = openAICompatibleClient()

    override fun invoke(request: DecideRequest): LLMResult<DecideResponse> {
        val parts = request.state.mapNotNull { it.toOpenAI() }
        if (parts.size != request.state.size) {
            return Failure(Internal(IllegalArgumentException("OpenAI only decides on text and binary images")))
        }

        return client.createDecision(
            request.model,
            listOf(DecisionInputMessage(parts)),
            request.questions.map { (name, question) -> question.toOpenAI(name) }
        )
            .map {
                DecideResponse(
                    it.answers.mapNotNull { answer -> answer.name?.let { name -> name to answer.toLLM() } }.toMap(),
                    DecideResponse.Metadata(it.model, TokenUsage(it.usage.input_tokens, it.usage.output_tokens, it.usage.total_tokens))
                )
            }
            .mapFailure { Http(Response(it.status).body(it.message ?: "")) }
    }
}

private fun Content.toOpenAI() = when (this) {
    is Content.Text -> DecisionInputPart.Text(text)
    is Content.Image -> (image as? Resource.Binary)?.asDataUrl()?.let { DecisionInputPart.Image(it, detail.toOpenAI()) }
    else -> null
}

private fun Resource.Binary.asDataUrl() = mimeType?.let { "data:${it.value};base64,${content.value}" }

private fun Content.Image.DetailLevel.toOpenAI() = when (this) {
    Content.Image.DetailLevel.Low -> ImageDetail.low
    Content.Image.DetailLevel.High -> ImageDetail.high
    Content.Image.DetailLevel.Auto -> ImageDetail.auto
}

private fun Question.toOpenAI(name: String) = when (this) {
    is Question.Choice -> DecisionQuestion.Choice(name, instructions, options.map { (value, description) -> DecisionChoice(value, description) })
    is Question.Score -> DecisionQuestion.Score(name, instructions, levels.map { DecisionLevel(it.label, it.description) })
    is Question.YesNo -> DecisionQuestion.Predicate(name, instructions)
}

private fun DecisionAnswer.toLLM() = when (this) {
    is DecisionAnswer.Predicate -> Answer.YesNo(probability)
    is DecisionAnswer.Choice -> Answer.Choice(choice, probabilities.associate { it.value to it.probability }, confidence)
    is DecisionAnswer.Score -> Answer.Score(score, probabilities.associate { it.value to it.probability }, confidence)
    is DecisionAnswer.Refusal -> Answer.Refusal
}
