package org.http4k.connect.azure

import dev.forkhandles.values.NonBlankStringValueFactory
import dev.forkhandles.values.StringValue
import org.http4k.ai.model.ApiKey
import org.http4k.ai.model.StopReason

class Region private constructor(value: String) : StringValue(value) {
    companion object : NonBlankStringValueFactory<Region>(::Region)
}

class ApiVersion private constructor(value: String) : StringValue(value) {
    companion object : NonBlankStringValueFactory<ApiVersion>(::ApiVersion) {
        val PREVIEW = ApiVersion.of("2024-04-01-preview")
    }
}

class AzureHost private constructor(value: String) : StringValue(value) {
    companion object : NonBlankStringValueFactory<AzureHost>(::AzureHost)
}

@Deprecated("use ApiKey", ReplaceWith("org.http4k.ai.model.ApiKey"))
typealias AzureAIApiKey = ApiKey

@Deprecated("use ApiKey", ReplaceWith("org.http4k.ai.model.ApiKey"))
typealias GitHubToken = ApiKey

class AzureResource private constructor(value: String) : StringValue(value) {
    companion object : NonBlankStringValueFactory<AzureResource>(::AzureResource)
}

class Deployment private constructor(value: String) : StringValue(value) {
    companion object : NonBlankStringValueFactory<Deployment>(::Deployment)
}

enum class ExtraParameters {
    `pass-through`, error, `ignore`
}

class ObjectType private constructor(value: String) : StringValue(value) {
    companion object : NonBlankStringValueFactory<ObjectType>(::ObjectType) {
        val List = ObjectType.of("list")
        val ChatCompletion = ObjectType.of("chat.completion")
        val ChatCompletionChunk = ObjectType.of("chat.completion.chunk")
        val Embedding = ObjectType.of("embedding")
    }
}

class ObjectId private constructor(value: String) : StringValue(value) {
    companion object : NonBlankStringValueFactory<ObjectId>(::ObjectId)
}

class TokenId private constructor(value: String) : StringValue(value) {
    companion object : NonBlankStringValueFactory<TokenId>(::TokenId)
}

class User private constructor(value: String) : StringValue(value) {
    companion object : NonBlankStringValueFactory<User>(::User)
}

class ModelProvider private constructor(value: String) : StringValue(value) {
    companion object : NonBlankStringValueFactory<ModelProvider>(::ModelProvider)
}

class ModelType private constructor(value: String) : StringValue(value) {
    companion object : NonBlankStringValueFactory<ModelType>(::ModelType)
}

val StopReason.Companion.stop get() = StopReason.of("stop")
val StopReason.Companion.length get() = StopReason.of("length")
val StopReason.Companion.content_filter get() = StopReason.of("content_filter")
val StopReason.Companion.tool_calls get() = StopReason.of("tool_calls")
