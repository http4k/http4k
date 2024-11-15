package org.http4k.connect.anthropic

import org.http4k.connect.FakeSystemContract
import org.http4k.core.Method.GET
import org.http4k.core.Request

class FakeAnthropicAIChaosTest : FakeSystemContract(FakeAnthropicAI()) {
    override val anyValid = Request(GET, "/v1/models")
}
