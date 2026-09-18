package org.http4k.connect.typesafe

import org.http4k.format.MoshiNode
import se.ansman.kotshi.JsonSerializable
import se.ansman.kotshi.Polymorphic
import se.ansman.kotshi.PolymorphicLabel

@JsonSerializable
@Polymorphic("type")
sealed class Answer {

    @JsonSerializable
    @PolymorphicLabel("choice")
    data class Choice(
        val choice: String,
        val probabilities: Map<String, Probability>,
        val confidence: Confidence
    ) : Answer()

    @JsonSerializable
    @PolymorphicLabel("score")
    data class Score(
        val score: Double,
        val legend: Map<String, MoshiNode>,
        val confidence: Confidence,
        val probabilities: Map<String, Probability>? = null
    ) : Answer()

    @JsonSerializable
    @PolymorphicLabel("noul")
    data class Noul(val noul: Probability) : Answer()
}
