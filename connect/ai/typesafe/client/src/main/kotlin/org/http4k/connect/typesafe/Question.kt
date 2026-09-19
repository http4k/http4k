package org.http4k.connect.typesafe

import org.http4k.format.MoshiNode
import se.ansman.kotshi.JsonSerializable
import se.ansman.kotshi.Polymorphic
import se.ansman.kotshi.PolymorphicLabel

@JsonSerializable
@Polymorphic("type")
sealed class Question<A : Answer> {
    abstract val instructions: MoshiNode

    @Transient
    internal open val id: QuestionId = QuestionId.of("unknown")

    internal abstract fun answerFrom(answer: Answer): A

    @JsonSerializable
    @PolymorphicLabel("choice")
    data class Choice(
        override val instructions: MoshiNode,
        val criteria: Map<String, MoshiNode>,
        @Transient override val id: QuestionId = QuestionId.of("unknown")
    ) : Question<Answer.Choice>() {
        override fun answerFrom(answer: Answer) =
            answer as? Answer.Choice ?: throw WrongAnswerType(id, "choice", answer)
    }

    @JsonSerializable
    @PolymorphicLabel("score")
    data class Score(
        override val instructions: MoshiNode,
        val criteria: List<MoshiNode>,
        @Transient override val id: QuestionId = QuestionId.of("unknown")
    ) : Question<Answer.Score>() {
        override fun answerFrom(answer: Answer) =
            answer as? Answer.Score ?: throw WrongAnswerType(id, "score", answer)
    }

    @JsonSerializable
    @PolymorphicLabel("noul")
    data class Noul(
        override val instructions: MoshiNode,
        val criteria: Map<String, MoshiNode>? = null,
        @Transient override val id: QuestionId = QuestionId.of("unknown")
    ) : Question<Answer.Noul>() {
        override fun answerFrom(answer: Answer) =
            answer as? Answer.Noul ?: throw WrongAnswerType(id, "noul", answer)
    }

    companion object {
        fun Choice(id: String, instructions: Any?, criteria: Map<String, Any?>) =
            Choice(instructions.asEntry(), criteria.mapValues { it.value.asEntry() }, QuestionId.of(id))

        fun Score(id: String, instructions: Any?, criteria: List<Any?>) =
            Score(instructions.asEntry(), criteria.map { it.asEntry() }, QuestionId.of(id))

        fun Noul(id: String, instructions: Any?, criteria: Map<String, Any?>? = null) =
            Noul(instructions.asEntry(), criteria?.mapValues { it.value.asEntry() }, QuestionId.of(id))
    }
}

class WrongAnswerType(id: QuestionId, expected: String, actual: Answer) : IllegalStateException(
    "Question '${id.value}' is a $expected but the API replied with ${actual::class.simpleName}"
)
