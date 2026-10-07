package org.http4k.connect.openai.action

import org.http4k.ai.model.ModelName
import org.http4k.connect.Http4kConnectAction
import org.http4k.connect.NonNullAutoMarshalledAction
import org.http4k.connect.kClass
import org.http4k.connect.openai.OpenAIAction
import org.http4k.connect.openai.OpenAIMoshi
import org.http4k.core.Method.POST
import org.http4k.core.Request
import org.http4k.core.with
import se.ansman.kotshi.JsonSerializable
import se.ansman.kotshi.Polymorphic
import se.ansman.kotshi.PolymorphicLabel

@Http4kConnectAction
@JsonSerializable
data class CreateDecision(
    val model: ModelName,
    val input: List<DecisionInputMessage>,
    val questions: List<DecisionQuestion>
) : NonNullAutoMarshalledAction<Decision>(kClass(), OpenAIMoshi), OpenAIAction<Decision> {
    constructor(model: ModelName, input: String, questions: List<DecisionQuestion>) :
        this(model, listOf(DecisionInputMessage(listOf(DecisionInputPart.Text(input)))), questions)

    override fun toRequest() = Request(POST, "/v1/decisions")
        .with(OpenAIMoshi.autoBody<CreateDecision>().toLens() of this)
}

@JsonSerializable
data class DecisionInputMessage(val content: List<DecisionInputPart>, val role: String = "user")

@JsonSerializable
@Polymorphic("type")
sealed class DecisionInputPart {
    @JsonSerializable
    @PolymorphicLabel("input_text")
    data class Text(val text: String) : DecisionInputPart()

    @JsonSerializable
    @PolymorphicLabel("input_image")
    data class Image(val image_url: String, val detail: ImageDetail? = null) : DecisionInputPart()
}

enum class ImageDetail { low, high, auto, original }

@JsonSerializable
@Polymorphic("type")
sealed class DecisionQuestion {
    abstract val name: String
    abstract val instructions: String

    @JsonSerializable
    @PolymorphicLabel("predicate")
    data class Predicate(override val name: String, override val instructions: String) : DecisionQuestion()

    @JsonSerializable
    @PolymorphicLabel("choice")
    data class Choice(
        override val name: String,
        override val instructions: String,
        val choices: List<DecisionChoice>
    ) : DecisionQuestion()

    @JsonSerializable
    @PolymorphicLabel("score")
    data class Score(
        override val name: String,
        override val instructions: String,
        val levels: List<DecisionLevel>
    ) : DecisionQuestion()
}

@JsonSerializable
data class DecisionChoice(val value: String, val description: String? = null)

@JsonSerializable
data class DecisionLevel(val label: String, val description: String? = null)

@JsonSerializable
data class Decision(val model: ModelName, val answers: List<DecisionAnswer>, val usage: DecisionUsage)

@JsonSerializable
data class DecisionUsage(val input_tokens: Int, val output_tokens: Int, val total_tokens: Int)

@JsonSerializable
@Polymorphic("type")
sealed class DecisionAnswer {
    abstract val name: String?

    @JsonSerializable
    @PolymorphicLabel("predicate")
    data class Predicate(override val name: String?, val probability: Double) : DecisionAnswer()

    @JsonSerializable
    @PolymorphicLabel("choice")
    data class Choice(
        override val name: String?,
        val choice: String,
        val probabilities: List<ChoiceProbability>,
        val confidence: Double
    ) : DecisionAnswer()

    @JsonSerializable
    @PolymorphicLabel("score")
    data class Score(
        override val name: String?,
        val score: Double,
        val probabilities: List<ScoreProbability>,
        val confidence: Double
    ) : DecisionAnswer()

    @JsonSerializable
    @PolymorphicLabel("refusal")
    data class Refusal(override val name: String?) : DecisionAnswer()

    @JsonSerializable
    data class ChoiceProbability(val value: String, val probability: Double)

    @JsonSerializable
    data class ScoreProbability(val value: Int, val label: String, val probability: Double)
}
