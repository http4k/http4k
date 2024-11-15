package org.http4k.connect.amazon.cognito

import org.http4k.connect.FakeSystemContract
import org.http4k.core.Method.GET
import org.http4k.core.Request

class FakeCognitoChaosTest : FakeSystemContract(FakeCognito()) {
    override val anyValid = Request(GET, "/")
}
