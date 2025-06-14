package org.http4k.connect.openai

import dev.forkhandles.values.NonBlankStringValueFactory
import dev.forkhandles.values.StringValue
import org.http4k.ai.model.ApiKey
import org.http4k.ai.model.ModelName
import org.http4k.ai.model.StopReason

@Deprecated("use ApiKey", ReplaceWith("org.http4k.ai.model.ApiKey"))
typealias OpenAIToken = ApiKey

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

enum class Quality {
    auto, standard, hd, high, medium, low
}

enum class Style {
    vivid, natural
}


object OpenAIModels {
    val GPT4 = ModelName.of("gpt-4")
    val DALL_E_2 = ModelName.of("dall-e-2")
    val GPT4_TURBO_PREVIEW = ModelName.of("gpt-4-turbo-preview")
    val GPT3_5 = ModelName.of("gpt-3.5-turbo")
    val TEXT_EMBEDDING_ADA_002 = ModelName.of("text-embedding-ada-002")
}

@Deprecated("Use OpenAiModels")
val ModelName.Companion.GPT4 get() = ModelName.of("gpt-4")

@Deprecated("Use OpenAiModels")
val ModelName.Companion.DALL_E_2 get() = ModelName.of("dall-e-2")

@Deprecated("Use OpenAiModels")
val ModelName.Companion.GPT4_TURBO_PREVIEW get() = ModelName.of("gpt-4-turbo-preview")

@Deprecated("Use OpenAiModels")
val ModelName.Companion.GPT3_5 get() = ModelName.of("gpt-3.5-turbo")

@Deprecated("Use OpenAiModels")
val ModelName.Companion.TEXT_EMBEDDING_ADA_002 get() = ModelName.of("text-embedding-ada-002")

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

