package org.http4k.connect.openai

import dev.forkhandles.values.NonBlankStringValueFactory
import dev.forkhandles.values.StringValue
import org.http4k.ai.model.ModelName
import org.http4k.ai.model.StopReason

class OpenAIOrg private constructor(value: String) : StringValue(value) {
    companion object : NonBlankStringValueFactory<OpenAIOrg>(::OpenAIOrg) {
        val ALL = OpenAIOrg.of("*")
        val OPENAI = OpenAIOrg.of("openai")
    }
}

class ObjectType private constructor(value: String) : StringValue(value) {
    companion object : NonBlankStringValueFactory<ObjectType>(::ObjectType) {
        val List = ObjectType.of("list")
        val Model = ObjectType.of("model")
        val ChatCompletion = ObjectType.of("chat.completion")
        val ChatCompletionChunk = ObjectType.of("chat.completion.chunk")
        val Embedding = ObjectType.of("embedding")
        val ModelPermission = ObjectType.of("model_permission")
    }
}

class ObjectId private constructor(value: String) : StringValue(value) {
    companion object : NonBlankStringValueFactory<ObjectId>(::ObjectId)
}

object OpenAIModels {
    val GPT_6_LUNA = ModelName.of("gpt-6-luna")
    val GPT_6_ASTRA = ModelName.of("gpt-6-astra")
    val GPT_6_1_SOL = ModelName.of("gpt-6.1-sol")
    val GPT_5_6_CYBER = ModelName.of("gpt-5.6-cyber")
    val GPT_IMAGE_2_5_SUNBURST = ModelName.of("gpt-image-2.5-sunburst")
    val GPT_IMAGE_2_5_FLARE = ModelName.of("gpt-image-2.5-flare")
    val TEXT_EMBEDDING_3_SMALL = ModelName.of("text-embedding-3-small")
    val TEXT_EMBEDDING_3_LARGE = ModelName.of("text-embedding-3-large")
}

class TokenId private constructor(value: String) : StringValue(value) {
    companion object : NonBlankStringValueFactory<TokenId>(::TokenId)
}

class User private constructor(value: String) : StringValue(value) {
    companion object : NonBlankStringValueFactory<User>(::User)
}

val StopReason.Companion.stop get() = StopReason.of("stop")
val StopReason.Companion.length get() = StopReason.of("length")
val StopReason.Companion.content_filter get() = StopReason.of("content_filter")
val StopReason.Companion.tool_calls get() = StopReason.of("tool_calls")
