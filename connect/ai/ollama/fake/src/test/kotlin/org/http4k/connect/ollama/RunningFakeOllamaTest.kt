package org.http4k.connect.ollama

import org.http4k.connect.WithRunningFake

class RunningFakeOllamaTest : OllamaContract, WithRunningFake(::FakeOllama) {
    override val ollama = Ollama.Http(http)
}
