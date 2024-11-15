package org.http4k.connect.openai

import org.http4k.connect.FakeSystemContract
import org.http4k.core.Method.GET
import org.http4k.core.Request

class FakeOpenAIChaosTest : FakeSystemContract(FakeOpenAI()) {
    override val anyValid = Request(GET, "/v1/models")
}
