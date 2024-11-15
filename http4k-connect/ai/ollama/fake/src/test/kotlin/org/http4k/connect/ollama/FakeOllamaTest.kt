package org.http4k.connect.ollama

import org.http4k.filter.debug

class FakeOllamaTest : OllamaContract {
    private val fakeOpenAI = FakeOllama()
    override val ollama = Ollama.Http(fakeOpenAI.debug())
}
