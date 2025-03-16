package org.http4k.connect

import org.http4k.connect.model.MaxTokens
import org.http4k.connect.model.MimeType
import org.http4k.connect.model.ModelName
import org.http4k.connect.model.Role
import org.http4k.connect.model.StopReason
import org.http4k.connect.model.SystemMessage
import org.http4k.connect.model.Temperature
import org.http4k.connect.model.ToolName
import org.http4k.format.AutoMappingConfiguration
import org.http4k.format.value

fun <T> AutoMappingConfiguration<T>.withAiMappings() = apply {
    value(MaxTokens)
    value(MimeType)
    value(ModelName)
    value(Role)
    value(StopReason)
    value(SystemMessage)
    value(Temperature)
    value(ToolName)
    value(MaxTokens)
}
