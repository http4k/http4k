package org.http4k.connect.amazon.eventbridge

import org.http4k.connect.FakeSystemContract
import org.http4k.core.Method.GET
import org.http4k.core.Request

class FakeEventBridgeChaosTest : FakeSystemContract(FakeEventBridge()) {
    override val anyValid = Request(GET, "/")
}
