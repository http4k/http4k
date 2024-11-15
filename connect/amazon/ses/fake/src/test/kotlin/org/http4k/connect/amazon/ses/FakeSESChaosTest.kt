package org.http4k.connect.amazon.ses

import org.http4k.connect.FakeSystemContract
import org.http4k.core.Method.POST
import org.http4k.core.Request

class FakeSESChaosTest : FakeSystemContract(FakeSES()) {
    override val anyValid = Request(POST, "/")
}
