package org.http4k.connect.amazon.cloudfront

import org.http4k.connect.FakeSystemContract
import org.http4k.core.Method.GET
import org.http4k.core.Request

class FakeCloudFrontChaosTest : FakeSystemContract(FakeCloudFront()) {
    override val anyValid = Request(GET, "/")
}
