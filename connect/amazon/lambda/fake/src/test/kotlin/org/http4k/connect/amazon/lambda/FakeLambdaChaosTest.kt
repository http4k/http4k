package org.http4k.connect.amazon.lambda

import org.http4k.connect.FakeSystemContract
import org.http4k.core.Method.POST
import org.http4k.core.Request

class FakeLambdaChaosTest : FakeSystemContract(FakeLambda(functions)) {
    override val anyValid = Request(POST, "/")
}
