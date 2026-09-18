package org.http4k.connect.typesafe

import org.http4k.connect.FakeSystemContract
import org.http4k.core.Method.GET
import org.http4k.core.Request

class FakeTypeSafeChaosTest : FakeSystemContract(FakeTypeSafe()) {
    override val anyValid = Request(GET, "/v1/models")
}
