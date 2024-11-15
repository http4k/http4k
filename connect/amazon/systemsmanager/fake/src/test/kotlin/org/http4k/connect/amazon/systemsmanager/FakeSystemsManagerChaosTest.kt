package org.http4k.connect.amazon.systemsmanager

import org.http4k.connect.FakeSystemContract
import org.http4k.core.Method.GET
import org.http4k.core.Request

class FakeSystemsManagerChaosTest : FakeSystemContract(FakeSystemsManager()) {
    override val anyValid = Request(GET, "/")
}
