package org.http4k.connect.amazon.iamidentitycenter

import org.http4k.connect.FakeSystemContract
import org.http4k.core.Method
import org.http4k.core.Request

class FakeSSOChaosTest : FakeSystemContract(FakeSSO()) {
    override val anyValid = Request(Method.GET, "/")
}
