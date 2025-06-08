package org.http4k.ai.llm.model

import org.http4k.ai.llm.util.LLMJsonNode
import se.ansman.kotshi.JsonSerializable
import se.ansman.kotshi.Polymorphic
import se.ansman.kotshi.PolymorphicLabel

@JsonSerializable
@Polymorphic("type")
sealed class ResponseFormat {
    @JsonSerializable
    @PolymorphicLabel("text")
    data object Text : ResponseFormat()

    @JsonSerializable
    @PolymorphicLabel("json")
    data class Json(val schema: LLMJsonNode) : ResponseFormat()
}
