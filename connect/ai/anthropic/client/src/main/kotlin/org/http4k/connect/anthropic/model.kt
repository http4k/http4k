package org.http4k.connect.anthropic

import dev.forkhandles.values.LocalDateValue
import dev.forkhandles.values.LocalDateValueFactory
import dev.forkhandles.values.NonBlankStringValueFactory
import dev.forkhandles.values.StringValue
import dev.forkhandles.values.Value
import org.http4k.ai.model.ApiKey
import org.http4k.ai.model.ModelName
import org.http4k.ai.model.StopReason
import org.http4k.ai.model.ToolName
import se.ansman.kotshi.JsonSerializable
import se.ansman.kotshi.Polymorphic
import se.ansman.kotshi.PolymorphicLabel
import java.time.LocalDate

@Deprecated("use ApiKey", ReplaceWith("org.http4k.ai.model.ApiKey"))
typealias AnthropicIApiKey = ApiKey

class UserId private constructor(value: String) : StringValue(value) {
    companion object : NonBlankStringValueFactory<UserId>(::UserId)
}

class ModelType private constructor(value: String) : StringValue(value) {
    companion object : NonBlankStringValueFactory<ModelType>(::ModelType)
}

class ToolUseId private constructor(value: String) : StringValue(value) {
    companion object : NonBlankStringValueFactory<ToolUseId>(::ToolUseId)
}

@JsonSerializable
@Polymorphic("type")
sealed class ToolChoice {
    @JsonSerializable
    @PolymorphicLabel("auto")
    data class Auto(val disable_parallel_tool_use: Boolean = false) : ToolChoice()

    @JsonSerializable
    @PolymorphicLabel("any")
    data class Any(val disable_parallel_tool_use: Boolean = false) : ToolChoice()

    @JsonSerializable
    @PolymorphicLabel("tool")
    data class Tool(val name: ToolName, val disable_parallel_tool_use: Boolean = false) : ToolChoice()

    @JsonSerializable
    @PolymorphicLabel("none")
    data object None : ToolChoice()
}

enum class SourceType {
    base64
}

val StopReason.Companion.end_turn get() = StopReason.of("end_turn")
val StopReason.Companion.max_tokens get() = StopReason.of("max_tokens")
val StopReason.Companion.stop_sequence get() = StopReason.of("stop_sequence")
val StopReason.Companion.tool_use get() = StopReason.of("tool_use")

class ApiVersion private constructor(value: LocalDate) : LocalDateValue(value), Value<LocalDate> {
    companion object : LocalDateValueFactory<ApiVersion>(::ApiVersion) {
        val _2023_06_01 = ApiVersion.parse("2023-06-01")
    }
}

object AnthropicModels {
    val Claude_Opus_4_1 = ModelName.of("claude-opus-4-1")
    val Claude_Sonnet_4_5 = ModelName.of("claude-sonnet-4-5")
    val Claude_Haiku_4_5 = ModelName.of("claude-haiku-4-5")
}
