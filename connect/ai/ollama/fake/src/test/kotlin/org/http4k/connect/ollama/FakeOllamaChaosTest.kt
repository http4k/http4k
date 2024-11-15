package org.http4k.connect.ollama

import org.http4k.connect.FakeSystemContract
import org.http4k.core.Method.GET
import org.http4k.core.Request

class FakeOllamaChaosTest : FakeSystemContract(FakeOllama()) {
    override val anyValid = Request(GET, "/api/ps")
}
