package org.http4k.ai.langchain4j.chat

import org.http4k.ai.model.ModelName
import org.http4k.connect.ollama.DEFAULT_OLLAMA_MODELS
import org.http4k.connect.ollama.FakeOllama
import org.http4k.connect.ollama.Http
import org.http4k.connect.ollama.Ollama

class OllamaChatLanguageModelTest : ChatLanguageModelContract {

    override val model by lazy {
        OllamaChatLanguageModel(
            Ollama.Http(FakeOllama()),
            ModelName.of(DEFAULT_OLLAMA_MODELS.keySet().first())
        )
    }
}
