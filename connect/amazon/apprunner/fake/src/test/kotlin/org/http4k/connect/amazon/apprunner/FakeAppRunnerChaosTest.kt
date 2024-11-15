package org.http4k.connect.amazon.apprunner

import org.http4k.connect.FakeSystemContract
import org.http4k.core.Method.POST
import org.http4k.core.Request

class FakeAppRunnerChaosTest : FakeSystemContract(FakeAppRunner()) {
    override val anyValid = Request(POST, "/")
}
