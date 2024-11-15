package org.http4k.connect.amazon.kms

import org.http4k.connect.FakeSystemContract
import org.http4k.core.Method.GET
import org.http4k.core.Request

class FakeKMSChaosTest : FakeSystemContract(FakeKMS()) {
    override val anyValid = Request(GET, "/")
}
