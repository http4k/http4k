package org.http4k.connect.langchain.chat

import org.http4k.client.JavaHttpClient
import org.http4k.connect.model.ModelName
import org.http4k.connect.ollama.Http
import org.http4k.connect.ollama.Ollama
import org.http4k.connect.ollama.action.ModelOptions
import org.http4k.core.Method.GET
import org.http4k.core.Request
import org.http4k.filter.debug
import org.http4k.util.PortBasedTest
import org.junit.jupiter.api.Assumptions.assumeTrue

class RealOllamaAiChatLanguageModelTest : ChatLanguageModelContract, PortBasedTest {

    init {
        assumeTrue(JavaHttpClient()(Request(GET, "http://localhost:11434/")).status.successful)
    }

    override val model by lazy {
        OllamaChatLanguageModel(
            Ollama.Http(JavaHttpClient().debug(debugStream = true)),
            ModelName.of("gemma:2b"),
            options = ModelOptions(temperature = 0.0)
        )
    }
}
