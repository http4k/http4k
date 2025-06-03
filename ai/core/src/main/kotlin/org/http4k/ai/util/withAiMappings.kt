package org.http4k.ai.util

import com.squareup.moshi.JsonAdapter
import org.http4k.ai.model.MaxTokens
import org.http4k.ai.model.ModelName
import org.http4k.ai.model.RequestId
import org.http4k.ai.model.ResponseId
import org.http4k.ai.model.Role
import org.http4k.ai.model.StopReason
import org.http4k.ai.model.SystemPrompt
import org.http4k.ai.model.Temperature
import org.http4k.ai.model.ToolName
import org.http4k.ai.model.UserPrompt
import org.http4k.connect.model.Base64Blob
import org.http4k.connect.model.MimeType
import org.http4k.connect.model.Timestamp
import org.http4k.format.AutoMappingConfiguration
import org.http4k.format.value
import se.ansman.kotshi.KotshiJsonAdapterFactory

fun <T> AutoMappingConfiguration<T>.withAiMappings() = apply {
    value(Base64Blob)
    value(MimeType)
    value(Timestamp)
    value(SystemPrompt)
    value(UserPrompt)
    value(ModelName)
    value(MaxTokens)
    value(ModelName)
    value(RequestId)
    value(ResponseId)
    value(Role)
    value(StopReason)
    value(Temperature)
    value(ToolName)
    value(MaxTokens)
}

@KotshiJsonAdapterFactory
object Http4kAiCoreJsonAdapterFactory : JsonAdapter.Factory by KotshiHttp4kAiCoreJsonAdapterFactory
