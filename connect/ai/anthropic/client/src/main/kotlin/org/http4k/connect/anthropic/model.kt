package org.http4k.connect.anthropic

import dev.forkhandles.values.LocalDateValue
import dev.forkhandles.values.LocalDateValueFactory
import dev.forkhandles.values.NonBlankStringValueFactory
import dev.forkhandles.values.StringValue
import dev.forkhandles.values.Value
import org.http4k.ai.model.ModelName
import org.http4k.ai.model.StopReason
import org.http4k.ai.model.ToolName
import se.ansman.kotshi.JsonSerializable
import se.ansman.kotshi.Polymorphic
import se.ansman.kotshi.PolymorphicLabel
import java.time.LocalDate

class UserId private constructor(value: String) : StringValue(value) {
    companion object : NonBlankStringValueFactory<UserId>(::UserId)
}

class ModelType private constructor(value: String) : StringValue(value) {
    companion object : NonBlankStringValueFactory<ModelType>(::ModelType)
}

class MessageBatchId private constructor(value: String) : StringValue(value) {
    companion object : NonBlankStringValueFactory<MessageBatchId>(::MessageBatchId)
}

class CustomId private constructor(value: String) : StringValue(value) {
    companion object : NonBlankStringValueFactory<CustomId>(::CustomId)
}

class ProcessingStatus private constructor(value: String) : StringValue(value) {
    companion object : NonBlankStringValueFactory<ProcessingStatus>(::ProcessingStatus) {
        val in_progress = of("in_progress")
        val canceling = of("canceling")
        val ended = of("ended")
    }
}

class SkillId private constructor(value: String) : StringValue(value) {
    companion object : NonBlankStringValueFactory<SkillId>(::SkillId)
}

class ToolType private constructor(value: String) : StringValue(value) {
    companion object : NonBlankStringValueFactory<ToolType>(::ToolType)
}

class ErrorCode private constructor(value: String) : StringValue(value) {
    companion object : NonBlankStringValueFactory<ErrorCode>(::ErrorCode)
}

class FileId private constructor(value: String) : StringValue(value) {
    companion object : NonBlankStringValueFactory<FileId>(::FileId)
}

class FileName private constructor(value: String) : StringValue(value) {
    companion object : NonBlankStringValueFactory<FileName>(::FileName)
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

@JsonSerializable
@Polymorphic("type")
sealed class Thinking {
    @JsonSerializable
    @PolymorphicLabel("adaptive")
    data class Adaptive(val display: ThinkingDisplay? = null) : Thinking()

    @JsonSerializable
    @PolymorphicLabel("disabled")
    data object Disabled : Thinking()

    @JsonSerializable
    @PolymorphicLabel("enabled")
    data class Enabled(val budget_tokens: Int, val display: ThinkingDisplay? = null) : Thinking()
}

enum class ThinkingDisplay {
    summarized, omitted, updates
}

val StopReason.Companion.end_turn get() = StopReason.of("end_turn")
val StopReason.Companion.max_tokens get() = StopReason.of("max_tokens")
val StopReason.Companion.stop_sequence get() = StopReason.of("stop_sequence")
val StopReason.Companion.tool_use get() = StopReason.of("tool_use")
val StopReason.Companion.refusal get() = StopReason.of("refusal")
val StopReason.Companion.pause_turn get() = StopReason.of("pause_turn")
val StopReason.Companion.model_context_window_exceeded get() = StopReason.of("model_context_window_exceeded")

class ApiVersion private constructor(value: LocalDate) : LocalDateValue(value), Value<LocalDate> {
    companion object : LocalDateValueFactory<ApiVersion>(::ApiVersion) {
        val _2023_06_01 = ApiVersion.parse("2023-06-01")
    }
}

object AnthropicModels {
    val Claude_Fable_5 = ModelName.of("claude-fable-5")
    val Claude_Mythos_5 = ModelName.of("claude-mythos-5")
    val Claude_Opus_5 = ModelName.of("claude-opus-5")
    val Claude_Opus_4_8 = ModelName.of("claude-opus-4-8")
    val Claude_Opus_4_7 = ModelName.of("claude-opus-4-7")
    val Claude_Opus_4_6 = ModelName.of("claude-opus-4-6")
    val Claude_Opus_4_1 = ModelName.of("claude-opus-4-1")
    val Claude_Sonnet_5 = ModelName.of("claude-sonnet-5")
    val Claude_Sonnet_4_6 = ModelName.of("claude-sonnet-4-6")
    val Claude_Sonnet_4_5 = ModelName.of("claude-sonnet-4-5")
    val Claude_Haiku_4_5 = ModelName.of("claude-haiku-4-5")
}
