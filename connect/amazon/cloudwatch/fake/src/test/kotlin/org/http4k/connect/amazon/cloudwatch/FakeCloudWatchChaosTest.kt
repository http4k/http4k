package org.http4k.connect.amazon.cloudwatch

import org.http4k.connect.FakeSystemContract
import org.http4k.core.Method.GET
import org.http4k.core.Request

class FakeCloudWatchChaosTest : FakeSystemContract(FakeCloudWatch()) {
    override val anyValid = Request(GET, "/")
}
