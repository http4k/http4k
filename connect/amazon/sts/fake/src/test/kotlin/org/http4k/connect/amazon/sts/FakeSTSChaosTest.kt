package org.http4k.connect.amazon.sts

import org.http4k.connect.FakeSystemContract
import org.http4k.core.Method.GET
import org.http4k.core.Request

class FakeSTSChaosTest : FakeSystemContract(FakeSTS()) {
    override val anyValid = Request(GET, "/")
}
