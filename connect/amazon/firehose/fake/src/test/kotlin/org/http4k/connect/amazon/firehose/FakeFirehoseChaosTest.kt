package org.http4k.connect.amazon.firehose

import org.http4k.connect.FakeSystemContract
import org.http4k.core.Method.GET
import org.http4k.core.Request

class FakeFirehoseChaosTest : FakeSystemContract(FakeFirehose()) {
    override val anyValid = Request(GET, "/")
}
