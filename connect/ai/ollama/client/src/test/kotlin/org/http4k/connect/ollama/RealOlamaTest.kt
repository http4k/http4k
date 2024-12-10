package org.http4k.connect.ollama

import org.http4k.client.JavaHttpClient
import org.http4k.core.Method.GET
import org.http4k.core.Request
import org.http4k.filter.debug
import org.http4k.util.PortBasedTest
import org.junit.jupiter.api.Assumptions.assumeTrue

class RealOllamaTest : OllamaContract, PortBasedTest {
    init {
        assumeTrue(JavaHttpClient()(Request(GET, "http://localhost:11434/")).status.successful)
    }

    override val ollama = Ollama.Http(JavaHttpClient().debug(debugStream = true))
}
