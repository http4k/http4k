package org.http4k.connect.amazon.containercredentials

import org.http4k.connect.FakeSystemContract
import org.http4k.core.Method.GET
import org.http4k.core.Request

class FakeContainerCredentialsChaosTest : FakeSystemContract(FakeContainerCredentials()) {
    override val anyValid = Request(GET, "/")
}
