package org.http4k.connect.langchain.chat

import org.http4k.connect.model.ModelName
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
