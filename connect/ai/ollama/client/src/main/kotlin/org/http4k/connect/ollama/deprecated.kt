package org.http4k.connect.ollama

import org.http4k.ai.model.SystemPrompt
import org.http4k.ai.model.UserPrompt

@Deprecated("use org.http4k.ai.model.Prompt")
typealias Prompt = UserPrompt

@Deprecated("use org.http4k.ai.model.SystemPrompt")
typealias SystemMessage = SystemPrompt

