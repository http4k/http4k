package org.http4k.connect.amazon.xray

import org.http4k.connect.FakeSystemContract
import org.http4k.core.Method.POST
import org.http4k.core.Request

class FakeXRayChaosTest : FakeSystemContract(FakeXRay()) {
    override val anyValid = Request(POST, "/TraceSummaries").body("""{"StartTime":1,"EndTime":2}""")
}
