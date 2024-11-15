package org.http4k.connect.amazon.sqs

import org.http4k.connect.FakeSystemContract
import org.http4k.core.Method.GET
import org.http4k.core.Request

class FakeSQSChaosTest : FakeSystemContract(FakeSQS()) {
    override val anyValid = Request(GET, "/")
}
