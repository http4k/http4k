package org.http4k.connect.amazon.iamidentitycenter

import org.http4k.connect.FakeSystemContract
import org.http4k.core.Method.GET
import org.http4k.core.Request

class FakeOIDCChaosTest : FakeSystemContract(FakeOIDC()) {
    override val anyValid = Request(GET, "/")
}
