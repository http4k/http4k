package org.http4k.connect.anthropic

import dev.forkhandles.values.LocalDateValue
import dev.forkhandles.values.LocalDateValueFactory
import dev.forkhandles.values.NonBlankStringValueFactory
import dev.forkhandles.values.StringValue
import dev.forkhandles.values.Value
import se.ansman.kotshi.JsonSerializable
import java.time.LocalDate

class AnthropicIApiKey private constructor(value: String) : StringValue(value) {
    companion object : NonBlankStringValueFactory<AnthropicIApiKey>(::AnthropicIApiKey)
}

class UserId private constructor(value: String) : StringValue(value) {
    companion object : NonBlankStringValueFactory<UserId>(::UserId)
}

class ModelType private constructor(value: String) : StringValue(value) {
    companion object : NonBlankStringValueFactory<ModelType>(::ModelType)
}

class MediaType private constructor(value: String) : StringValue(value) {
    companion object : NonBlankStringValueFactory<MediaType>(::MediaType) {
        val IMAGE_JPG = MediaType.of("image/jpeg")
        val IMAGE_PNG = MediaType.of("image/png")
        val IMAGE_GIF = MediaType.of("image/gif")
        val IMAGE_WEBP = MediaType.of("image/webp")
    }
}

class Prompt private constructor(value: String) : StringValue(value) {
    companion object : NonBlankStringValueFactory<Prompt>(::Prompt)
}

class ToolName private constructor(value: String) : StringValue(value) {
    companion object : NonBlankStringValueFactory<ToolName>(::ToolName)
}


@JsonSerializable
data class ToolChoice(
    val type: ToolType
)

enum class SourceType {
    base64
}

enum class Type {
    text, image
}

enum class ToolType {
    auto,
    any,
    specific_tool
}

class ResponseId private constructor(value: String) : StringValue(value) {
    companion object : NonBlankStringValueFactory<ResponseId>(::ResponseId)
}

enum class StopReason {
    end_turn,
    max_tokens,
    stop_sequence,
    tool_use
}

class ApiVersion private constructor(value: LocalDate) : LocalDateValue(value), Value<LocalDate> {
    companion object : LocalDateValueFactory<ApiVersion>(::ApiVersion) {
        val _2023_06_01 = ApiVersion.parse("2023-06-01")
    }
}
