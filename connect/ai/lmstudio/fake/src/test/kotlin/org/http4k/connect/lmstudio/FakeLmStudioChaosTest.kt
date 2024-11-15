package org.http4k.connect.lmstudio

import org.http4k.connect.FakeSystemContract
import org.http4k.core.Method.GET
import org.http4k.core.Request

class FakeLmStudioChaosTest : FakeSystemContract(FakeLmStudio()) {
    override val anyValid = Request(GET, "/v1/models")
}
