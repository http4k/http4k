package org.http4k.ai.llm.chat

import org.http4k.ai.llm.util.LLMJsonNode
import se.ansman.kotshi.JsonSerializable
import se.ansman.kotshi.Polymorphic
import se.ansman.kotshi.PolymorphicLabel

@JsonSerializable
@Polymorphic("type")
sealed class ChatResponseFormat {
    @JsonSerializable
    @PolymorphicLabel("text")
    data object Text : ChatResponseFormat()

    @JsonSerializable
    @PolymorphicLabel("json")
    data class Json(val schema: LLMJsonNode) : ChatResponseFormat()
}
