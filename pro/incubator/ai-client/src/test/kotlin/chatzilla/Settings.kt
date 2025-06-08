package chatzilla

import org.http4k.ai.model.ModelName
import org.http4k.config.EnvironmentKey
import org.http4k.connect.anthropic.AnthropicIApiKey
import org.http4k.lens.of
import org.http4k.lens.uri
import org.http4k.lens.value

object Settings {
    val MCP_URL by EnvironmentKey.uri().of().required()
    val MODEL by EnvironmentKey.value(ModelName).of().required()
    val ANTHROPIC_API_KEY by EnvironmentKey.value(AnthropicIApiKey).of().required()
}
