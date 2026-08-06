package org.http4k.connect.amazon.iot

import org.http4k.connect.FakeSystemContract
import org.http4k.core.Method.GET
import org.http4k.core.Request

class FakeIotChaosTest : FakeSystemContract(FakeIot()) {
    override val anyValid = Request(GET, "/endpoint")
}
